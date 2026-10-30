package core

import (
	"testing"

	"github.com/sagernet/sing-box/option"
)

func TestApplyRuntimeOwnsTheControllerEndpoint(t *testing.T) {
	var options option.Options
	address, err := applyRuntime(&options, Options{Secret: "s3cret"})
	if err != nil {
		t.Fatalf("applyRuntime: %v", err)
	}
	api := options.Experimental.ClashAPI
	if api == nil {
		t.Fatal("the clash api options must be created")
	}
	if api.ExternalController != address {
		t.Fatalf("controller address: got %q want %q", api.ExternalController, address)
	}
	if api.Secret != "s3cret" {
		t.Fatalf("secret: got %q", api.Secret)
	}
	// YumeBox's /configs decoder only knows lowercase serial names.
	if api.DefaultMode != "rule" {
		t.Fatalf("default mode must be lowercase for the app decoder, got %q", api.DefaultMode)
	}
}

func TestApplyRuntimeOverridesProfileController(t *testing.T) {
	// A profile that ships its own controller must not win: the app owns the endpoint.
	options := option.Options{
		Experimental: &option.ExperimentalOptions{
			ClashAPI: &option.ClashAPIOptions{
				ExternalController: "0.0.0.0:20123",
				Secret:             "profile-secret",
			},
		},
	}
	address, err := applyRuntime(&options, Options{})
	if err != nil {
		t.Fatalf("applyRuntime: %v", err)
	}
	api := options.Experimental.ClashAPI
	if api.ExternalController == "0.0.0.0:20123" {
		t.Fatal("the profile controller address must be replaced by the app-owned one")
	}
	if api.ExternalController != address {
		t.Fatalf("controller address: got %q want %q", api.ExternalController, address)
	}
}

func TestApplyRuntimeMapsTunStack(t *testing.T) {
	tunOptions := &option.TunInboundOptions{Stack: "system"}
	options := option.Options{
		Inbounds: []option.Inbound{{Type: "tun", Options: tunOptions}},
	}
	if _, err := applyRuntime(&options, Options{Stack: "mixed"}); err != nil {
		t.Fatalf("applyRuntime: %v", err)
	}
	// mihomo accepted "mixed" and fell back to gvisor; sing-box only knows two stacks.
	if tunOptions.Stack != "gvisor" {
		t.Fatalf("launcher stack mixed must map to gvisor, got %q", tunOptions.Stack)
	}

	tunOptions.Stack = "system"
	if _, err := applyRuntime(&options, Options{Stack: "system"}); err != nil {
		t.Fatalf("applyRuntime: %v", err)
	}
	if tunOptions.Stack != "system" {
		t.Fatalf("launcher stack system must be preserved, got %q", tunOptions.Stack)
	}
}
