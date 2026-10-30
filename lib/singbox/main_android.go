//go:build android && cgo

package main

/*
#include <stdint.h>
*/
import "C"

import (
	"unsafe"

	"github.com/yumeyucca/yumebox/singbox/internal/core"
)

//export SingboxMain
func SingboxMain(argc C.int, argv **C.char, channel *C.char) C.int {
	count := int(argc)
	if count <= 0 || argv == nil {
		return C.int(core.Fatal("launcher supplied an empty argument vector"))
	}
	cArgs := unsafe.Slice(argv, count)
	args := make([]string, count)
	for index, value := range cArgs {
		args[index] = C.GoString(value)
	}
	channelValue := ""
	if channel != nil {
		channelValue = C.GoString(channel)
	}
	return C.int(core.Run(args, channelValue))
}
