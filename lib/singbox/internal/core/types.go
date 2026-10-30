package core

// ownerQuery resolves a connection to (uid, package), or (-1, "") when unknown; nil when the
// process has no launcher channel (root modes).
type ownerQuery func(protocol int, source, target string) (uid int, pkg string)
