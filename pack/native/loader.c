#include <jni.h>
#include <lzma.h>

#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#define ZIP_LOCAL_HEADER 0x04034b50U
#define ZIP_CENTRAL_HEADER 0x02014b50U
#define ZIP_END_HEADER 0x06054b50U
#define IO_BUFFER_SIZE (256U * 1024U)

typedef struct {
    uint32_t state[8];
    uint64_t bit_count;
    uint8_t block[64];
    size_t block_size;
} Sha256;

typedef struct {
    uint64_t data_offset;
    uint64_t compressed_size;
} ZipEntryLocation;

typedef struct {
    int fd;
    uint64_t file_size;
    uint8_t *central_directory;
    size_t central_size;
    uint32_t entry_count;
} NativeArchive;

static uint32_t read_u16(const uint8_t *value) {
    return (uint32_t)value[0] | ((uint32_t)value[1] << 8U);
}

static uint32_t read_u32(const uint8_t *value) {
    return (uint32_t)value[0]
            | ((uint32_t)value[1] << 8U)
            | ((uint32_t)value[2] << 16U)
            | ((uint32_t)value[3] << 24U);
}

static void set_error(char *error, size_t capacity, const char *format, ...) {
    va_list args;
    va_start(args, format);
    vsnprintf(error, capacity, format, args);
    va_end(args);
}

static bool read_exact_at(int fd, uint64_t offset, void *buffer, size_t size) {
    uint8_t *next = buffer;
    size_t remaining = size;
    while (remaining > 0) {
        ssize_t count = pread(fd, next, remaining, (off_t)offset);
        if (count < 0 && errno == EINTR) {
            continue;
        }
        if (count <= 0) {
            return false;
        }
        next += count;
        offset += (uint64_t)count;
        remaining -= (size_t)count;
    }
    return true;
}

static bool write_all(int fd, const uint8_t *buffer, size_t size) {
    while (size > 0) {
        ssize_t count = write(fd, buffer, size);
        if (count < 0 && errno == EINTR) {
            continue;
        }
        if (count <= 0) {
            return false;
        }
        buffer += count;
        size -= (size_t)count;
    }
    return true;
}

static uint32_t rotate_right(uint32_t value, unsigned count) {
    return (value >> count) | (value << (32U - count));
}

static void sha256_transform(Sha256 *sha, const uint8_t block[64]) {
    static const uint32_t constants[64] = {
        0x428a2f98U, 0x71374491U, 0xb5c0fbcfU, 0xe9b5dba5U,
        0x3956c25bU, 0x59f111f1U, 0x923f82a4U, 0xab1c5ed5U,
        0xd807aa98U, 0x12835b01U, 0x243185beU, 0x550c7dc3U,
        0x72be5d74U, 0x80deb1feU, 0x9bdc06a7U, 0xc19bf174U,
        0xe49b69c1U, 0xefbe4786U, 0x0fc19dc6U, 0x240ca1ccU,
        0x2de92c6fU, 0x4a7484aaU, 0x5cb0a9dcU, 0x76f988daU,
        0x983e5152U, 0xa831c66dU, 0xb00327c8U, 0xbf597fc7U,
        0xc6e00bf3U, 0xd5a79147U, 0x06ca6351U, 0x14292967U,
        0x27b70a85U, 0x2e1b2138U, 0x4d2c6dfcU, 0x53380d13U,
        0x650a7354U, 0x766a0abbU, 0x81c2c92eU, 0x92722c85U,
        0xa2bfe8a1U, 0xa81a664bU, 0xc24b8b70U, 0xc76c51a3U,
        0xd192e819U, 0xd6990624U, 0xf40e3585U, 0x106aa070U,
        0x19a4c116U, 0x1e376c08U, 0x2748774cU, 0x34b0bcb5U,
        0x391c0cb3U, 0x4ed8aa4aU, 0x5b9cca4fU, 0x682e6ff3U,
        0x748f82eeU, 0x78a5636fU, 0x84c87814U, 0x8cc70208U,
        0x90befffaU, 0xa4506cebU, 0xbef9a3f7U, 0xc67178f2U,
    };
    uint32_t words[64];
    for (size_t i = 0; i < 16; ++i) {
        words[i] = ((uint32_t)block[i * 4] << 24U)
                | ((uint32_t)block[i * 4 + 1] << 16U)
                | ((uint32_t)block[i * 4 + 2] << 8U)
                | (uint32_t)block[i * 4 + 3];
    }
    for (size_t i = 16; i < 64; ++i) {
        uint32_t s0 = rotate_right(words[i - 15], 7) ^ rotate_right(words[i - 15], 18)
                ^ (words[i - 15] >> 3U);
        uint32_t s1 = rotate_right(words[i - 2], 17) ^ rotate_right(words[i - 2], 19)
                ^ (words[i - 2] >> 10U);
        words[i] = words[i - 16] + s0 + words[i - 7] + s1;
    }

    uint32_t a = sha->state[0];
    uint32_t b = sha->state[1];
    uint32_t c = sha->state[2];
    uint32_t d = sha->state[3];
    uint32_t e = sha->state[4];
    uint32_t f = sha->state[5];
    uint32_t g = sha->state[6];
    uint32_t h = sha->state[7];
    for (size_t i = 0; i < 64; ++i) {
        uint32_t sum1 = rotate_right(e, 6) ^ rotate_right(e, 11) ^ rotate_right(e, 25);
        uint32_t choice = (e & f) ^ ((~e) & g);
        uint32_t temp1 = h + sum1 + choice + constants[i] + words[i];
        uint32_t sum0 = rotate_right(a, 2) ^ rotate_right(a, 13) ^ rotate_right(a, 22);
        uint32_t majority = (a & b) ^ (a & c) ^ (b & c);
        uint32_t temp2 = sum0 + majority;
        h = g;
        g = f;
        f = e;
        e = d + temp1;
        d = c;
        c = b;
        b = a;
        a = temp1 + temp2;
    }
    sha->state[0] += a;
    sha->state[1] += b;
    sha->state[2] += c;
    sha->state[3] += d;
    sha->state[4] += e;
    sha->state[5] += f;
    sha->state[6] += g;
    sha->state[7] += h;
}

static void sha256_init(Sha256 *sha) {
    *sha = (Sha256){
        .state = {
            0x6a09e667U, 0xbb67ae85U, 0x3c6ef372U, 0xa54ff53aU,
            0x510e527fU, 0x9b05688cU, 0x1f83d9abU, 0x5be0cd19U,
        },
    };
}

static void sha256_update(Sha256 *sha, const uint8_t *data, size_t size) {
    sha->bit_count += (uint64_t)size * 8U;
    while (size > 0) {
        size_t available = sizeof(sha->block) - sha->block_size;
        size_t count = size < available ? size : available;
        memcpy(sha->block + sha->block_size, data, count);
        sha->block_size += count;
        data += count;
        size -= count;
        if (sha->block_size == sizeof(sha->block)) {
            sha256_transform(sha, sha->block);
            sha->block_size = 0;
        }
    }
}

static void sha256_finish(Sha256 *sha, uint8_t output[32]) {
    uint64_t bit_count = sha->bit_count;
    sha->block[sha->block_size++] = 0x80;
    if (sha->block_size > 56) {
        memset(sha->block + sha->block_size, 0, 64 - sha->block_size);
        sha256_transform(sha, sha->block);
        sha->block_size = 0;
    }
    memset(sha->block + sha->block_size, 0, 56 - sha->block_size);
    for (size_t i = 0; i < 8; ++i) {
        sha->block[63 - i] = (uint8_t)(bit_count >> (i * 8U));
    }
    sha256_transform(sha, sha->block);
    for (size_t i = 0; i < 8; ++i) {
        output[i * 4] = (uint8_t)(sha->state[i] >> 24U);
        output[i * 4 + 1] = (uint8_t)(sha->state[i] >> 16U);
        output[i * 4 + 2] = (uint8_t)(sha->state[i] >> 8U);
        output[i * 4 + 3] = (uint8_t)sha->state[i];
    }
}

static NativeArchive *open_native_archive(
        const char *apk_path,
        char *error,
        size_t error_capacity) {
    NativeArchive *archive = calloc(1, sizeof(*archive));
    if (archive == NULL) {
        set_error(error, error_capacity, "allocate APK archive");
        return NULL;
    }
    archive->fd = open(apk_path, O_RDONLY | O_CLOEXEC);
    if (archive->fd < 0) {
        set_error(error, error_capacity, "open APK: %s", strerror(errno));
        free(archive);
        return NULL;
    }
    (void)posix_fadvise(archive->fd, 0, 0, POSIX_FADV_SEQUENTIAL);
    struct stat status;
    if (fstat(archive->fd, &status) != 0 || status.st_size < 22) {
        set_error(error, error_capacity, "invalid APK: %s", strerror(errno));
        close(archive->fd);
        free(archive);
        return NULL;
    }
    archive->file_size = (uint64_t)status.st_size;
    size_t tail_size = archive->file_size < 65557U ? (size_t)archive->file_size : 65557U;
    uint8_t *tail = malloc(tail_size);
    if (tail == NULL) {
        set_error(error, error_capacity, "allocate ZIP footer buffer");
        close(archive->fd);
        free(archive);
        return NULL;
    }
    if (!read_exact_at(archive->fd, archive->file_size - tail_size, tail, tail_size)) {
        set_error(error, error_capacity, "read APK ZIP footer: %s", strerror(errno));
        free(tail);
        close(archive->fd);
        free(archive);
        return NULL;
    }

    const uint8_t *end = NULL;
    for (size_t offset = tail_size - 22;; --offset) {
        if (read_u32(tail + offset) == ZIP_END_HEADER
                && offset + 22U + read_u16(tail + offset + 20) == tail_size) {
            end = tail + offset;
            break;
        }
        if (offset == 0) {
            break;
        }
    }
    if (end == NULL) {
        set_error(error, error_capacity, "APK ZIP end record is missing");
        free(tail);
        close(archive->fd);
        free(archive);
        return NULL;
    }
    uint32_t entry_count = read_u16(end + 10);
    uint32_t central_size = read_u32(end + 12);
    uint32_t central_offset = read_u32(end + 16);
    free(tail);
    if (entry_count == 0xffffU || central_size == 0xffffffffU
            || central_offset == 0xffffffffU
            || (uint64_t)central_offset + central_size > archive->file_size) {
        set_error(error, error_capacity, "unsupported or invalid ZIP64 APK");
        close(archive->fd);
        free(archive);
        return NULL;
    }
    archive->central_directory = malloc(central_size);
    if (archive->central_directory == NULL) {
        set_error(error, error_capacity, "allocate ZIP central directory");
        close(archive->fd);
        free(archive);
        return NULL;
    }
    if (!read_exact_at(archive->fd, central_offset, archive->central_directory, central_size)) {
        set_error(error, error_capacity, "read ZIP central directory: %s", strerror(errno));
        free(archive->central_directory);
        close(archive->fd);
        free(archive);
        return NULL;
    }
    archive->central_size = central_size;
    archive->entry_count = entry_count;
    return archive;
}

static void close_native_archive(NativeArchive *archive) {
    if (archive == NULL) {
        return;
    }
    free(archive->central_directory);
    close(archive->fd);
    free(archive);
}

static bool locate_zip_entry(
        NativeArchive *archive,
        const char *entry_name,
        ZipEntryLocation *location,
        char *error,
        size_t error_capacity) {
    size_t cursor = 0;
    size_t wanted_length = strlen(entry_name);
    for (uint32_t index = 0; index < archive->entry_count; ++index) {
        if (cursor + 46U > archive->central_size) {
            set_error(error, error_capacity, "invalid ZIP central directory bounds");
            return false;
        }
        const uint8_t *header = archive->central_directory + cursor;
        if (read_u32(header) != ZIP_CENTRAL_HEADER) {
            set_error(error, error_capacity, "invalid ZIP central directory entry");
            return false;
        }
        uint32_t method = read_u16(header + 10);
        uint32_t compressed_size = read_u32(header + 20);
        uint32_t name_length = read_u16(header + 28);
        uint32_t extra_length = read_u16(header + 30);
        uint32_t comment_length = read_u16(header + 32);
        uint32_t local_offset = read_u32(header + 42);
        size_t next = cursor + 46U + name_length + extra_length + comment_length;
        if (next > archive->central_size) {
            set_error(error, error_capacity, "invalid ZIP central directory bounds");
            return false;
        }

        bool matches = name_length == wanted_length
                && memcmp(header + 46U, entry_name, name_length) == 0;
        if (matches) {
            if (method != 0) {
                set_error(error, error_capacity, "payload ZIP entry is not Stored: %s", entry_name);
                return false;
            }
            uint8_t local[30];
            if (local_offset == 0xffffffffU
                    || !read_exact_at(archive->fd, local_offset, local, sizeof(local))
                    || read_u32(local) != ZIP_LOCAL_HEADER) {
                set_error(error, error_capacity, "invalid ZIP local header for %s", entry_name);
                return false;
            }
            uint64_t data_offset = (uint64_t)local_offset + sizeof(local)
                    + read_u16(local + 26) + read_u16(local + 28);
            if (data_offset + compressed_size > archive->file_size) {
                set_error(error, error_capacity, "invalid ZIP payload bounds for %s", entry_name);
                return false;
            }
            location->data_offset = data_offset;
            location->compressed_size = compressed_size;
            return true;
        }
        cursor = next;
    }
    set_error(error, error_capacity, "payload entry is missing: %s", entry_name);
    return false;
}

static bool extract_payload(
        NativeArchive *archive,
        const char *entry_name,
        const char *target_path,
        uint64_t expected_size,
        const uint8_t expected_sha256[32],
        char *error,
        size_t error_capacity) {
    bool success = false;
    int output = -1;
    uint8_t *input_buffer = NULL;
    uint8_t *output_buffer = NULL;
    lzma_stream stream = LZMA_STREAM_INIT;
    bool stream_initialized = false;

    ZipEntryLocation location;
    if (!locate_zip_entry(archive, entry_name, &location, error, error_capacity)) {
        goto cleanup;
    }
    output = open(target_path, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC, 0600);
    if (output < 0) {
        set_error(error, error_capacity, "create payload: %s", strerror(errno));
        goto cleanup;
    }
    input_buffer = malloc(IO_BUFFER_SIZE);
    output_buffer = malloc(IO_BUFFER_SIZE);
    if (input_buffer == NULL || output_buffer == NULL) {
        set_error(error, error_capacity, "allocate decompression buffers");
        goto cleanup;
    }
    // The uncompressed bytes are hashed below, so the xz container check is redundant work.
    lzma_ret result = lzma_stream_decoder(&stream, UINT64_MAX, LZMA_IGNORE_CHECK);
    if (result != LZMA_OK) {
        set_error(error, error_capacity, "initialize liblzma decoder: %d", result);
        goto cleanup;
    }
    stream_initialized = true;

    Sha256 sha;
    sha256_init(&sha);
    uint64_t input_offset = 0;
    uint64_t output_size = 0;
    while (true) {
        if (stream.avail_in == 0 && input_offset < location.compressed_size) {
            size_t count = (size_t)(location.compressed_size - input_offset);
            if (count > IO_BUFFER_SIZE) {
                count = IO_BUFFER_SIZE;
            }
            if (!read_exact_at(archive->fd, location.data_offset + input_offset, input_buffer, count)) {
                set_error(error, error_capacity, "read compressed payload: %s", strerror(errno));
                goto cleanup;
            }
            input_offset += count;
            stream.next_in = input_buffer;
            stream.avail_in = count;
        }

        stream.next_out = output_buffer;
        stream.avail_out = IO_BUFFER_SIZE;
        lzma_action action = input_offset == location.compressed_size && stream.avail_in == 0
                ? LZMA_FINISH : LZMA_RUN;
        result = lzma_code(&stream, action);
        size_t produced = IO_BUFFER_SIZE - stream.avail_out;
        if (produced > 0) {
            if (!write_all(output, output_buffer, produced)) {
                set_error(error, error_capacity, "write payload: %s", strerror(errno));
                goto cleanup;
            }
            sha256_update(&sha, output_buffer, produced);
            output_size += produced;
        }
        if (result == LZMA_STREAM_END) {
            break;
        }
        if (result != LZMA_OK || (action == LZMA_FINISH && produced == 0)) {
            set_error(error, error_capacity, "decompress payload with liblzma: %d", result);
            goto cleanup;
        }
    }

    uint8_t actual_sha256[32];
    sha256_finish(&sha, actual_sha256);
    if (output_size != expected_size || memcmp(actual_sha256, expected_sha256, 32) != 0) {
        set_error(error, error_capacity, "payload verification failed: size %llu, expected %llu",
                (unsigned long long)output_size, (unsigned long long)expected_size);
        goto cleanup;
    }
    if (fdatasync(output) != 0) {
        set_error(error, error_capacity, "sync payload: %s", strerror(errno));
        goto cleanup;
    }
    success = true;

cleanup:
    if (stream_initialized) {
        lzma_end(&stream);
    }
    free(input_buffer);
    free(output_buffer);
    if (output >= 0) {
        close(output);
    }
    if (!success) {
        unlink(target_path);
    }
    return success;
}

static void throw_io_exception(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) {
        return;
    }
    jclass exception = (*env)->FindClass(env, "java/io/IOException");
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
    }
}

__attribute__((visibility("default"))) JNIEXPORT jlong JNICALL
Java_dev_yume_loader_NativePayloadExtractor_openArchive(
        JNIEnv *env,
        jclass type,
        jstring apk_path_value) {
    (void)type;
    if (apk_path_value == NULL) {
        throw_io_exception(env, "APK path is missing");
        return 0;
    }
    const char *apk_path = (*env)->GetStringUTFChars(env, apk_path_value, NULL);
    if (apk_path == NULL) {
        return 0;
    }
    char error[512] = {0};
    NativeArchive *archive = open_native_archive(apk_path, error, sizeof(error));
    (*env)->ReleaseStringUTFChars(env, apk_path_value, apk_path);
    if (archive == NULL) {
        throw_io_exception(env, error);
        return 0;
    }
    return (jlong)(uintptr_t)archive;
}

__attribute__((visibility("default"))) JNIEXPORT void JNICALL
Java_dev_yume_loader_NativePayloadExtractor_extract(
        JNIEnv *env,
        jclass type,
        jlong archive_value,
        jstring entry_name_value,
        jstring target_path_value,
        jlong expected_size,
        jbyteArray expected_sha256_value) {
    (void)type;
    char error[512] = {0};
    const char *entry_name = NULL;
    const char *target_path = NULL;
    uint8_t expected_sha256[32];

    NativeArchive *archive = (NativeArchive *)(uintptr_t)archive_value;
    if (archive == NULL || entry_name_value == NULL || target_path_value == NULL
            || expected_sha256_value == NULL || expected_size < 0
            || (*env)->GetArrayLength(env, expected_sha256_value) != 32) {
        set_error(error, sizeof(error), "invalid payload metadata");
        goto finish;
    }
    entry_name = (*env)->GetStringUTFChars(env, entry_name_value, NULL);
    if (entry_name == NULL) {
        goto finish;
    }
    target_path = (*env)->GetStringUTFChars(env, target_path_value, NULL);
    if (target_path == NULL) {
        goto finish;
    }
    (*env)->GetByteArrayRegion(env, expected_sha256_value, 0, 32, (jbyte *)expected_sha256);
    if ((*env)->ExceptionCheck(env)) {
        goto finish;
    }
    if (extract_payload(archive, entry_name, target_path, (uint64_t)expected_size,
            expected_sha256, error, sizeof(error))) {
        goto finish;
    }

finish:
    if (target_path != NULL) {
        (*env)->ReleaseStringUTFChars(env, target_path_value, target_path);
    }
    if (entry_name != NULL) {
        (*env)->ReleaseStringUTFChars(env, entry_name_value, entry_name);
    }
    if (error[0] != '\0') {
        throw_io_exception(env, error);
    }
}

__attribute__((visibility("default"))) JNIEXPORT void JNICALL
Java_dev_yume_loader_NativePayloadExtractor_closeArchive(
        JNIEnv *env,
        jclass type,
        jlong archive_value) {
    (void)env;
    (void)type;
    close_native_archive((NativeArchive *)(uintptr_t)archive_value);
}
