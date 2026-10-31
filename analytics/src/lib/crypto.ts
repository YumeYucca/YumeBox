const encoder = new TextEncoder();

function toHex(value: ArrayBuffer): string {
  return Array.from(new Uint8Array(value), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function sha256(value: string): Promise<ArrayBuffer> {
  return crypto.subtle.digest("SHA-256", encoder.encode(value));
}

/** The client UUID is never persisted; only this keyed, irreversible identifier is stored. */
export async function hashDeviceId(deviceId: string, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(deviceId));
  return toHex(signature);
}

/** Hashing first keeps the comparison length-independent and avoids early-exit token comparison. */
export async function tokensEqual(received: string, expected: string): Promise<boolean> {
  const [left, right] = await Promise.all([sha256(received), sha256(expected)]);
  const leftBytes = new Uint8Array(left);
  const rightBytes = new Uint8Array(right);
  let difference = 0;
  for (let index = 0; index < leftBytes.length; index += 1) {
    difference |= leftBytes[index]! ^ rightBytes[index]!;
  }
  return difference === 0;
}
