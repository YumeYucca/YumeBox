if (location.hostname === 'yumebox.mintlify.app') {
	const url = new URL(location.href)
	location.replace(((url.hostname = 'yumebox.yumeyuka.moe'), url))
}