#!/usr/bin/env python3
"""Minimal docker API client over unix socket: exec, create, start, logs, pull."""
import http.client, json, socket, sys, struct

SOCK = "/var/run/docker.sock"

class UnixHTTP(http.client.HTTPConnection):
    def __init__(self):
        super().__init__("localhost")
    def connect(self):
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.connect(SOCK)
        self.sock = s

def req(method, path, body=None, timeout=None):
    c = UnixHTTP()
    headers = {}
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    c.request(method, path, body=data, headers=headers)
    r = c.getresponse()
    raw = r.read()
    c.close()
    return r.status, raw

def demux(raw):
    """Strip docker stream multiplex headers."""
    out = []
    i = 0
    while i + 8 <= len(raw):
        _, _, _, _, size = struct.unpack(">BBBBI", raw[i:i+8])
        out.append(raw[i+8:i+8+size])
        i += 8 + size
    return b"".join(out)

def dexec(container, cmd, stdin_data=None):
    body = {"AttachStdout": True, "AttachStderr": True, "Cmd": cmd}
    if stdin_data is not None:
        body["AttachStdin"] = True
    st, raw = req("POST", f"/containers/{container}/exec", body)
    if st != 201:
        print(f"exec create failed {st}: {raw.decode()}", file=sys.stderr)
        sys.exit(1)
    eid = json.loads(raw)["Id"]
    # start exec with stream hijack
    c = UnixHTTP()
    payload = json.dumps({"Detach": False, "Tty": False}).encode()
    c.putrequest("POST", f"/exec/{eid}/start")
    c.putheader("Content-Type", "application/json")
    c.putheader("Content-Length", str(len(payload)))
    if stdin_data is not None:
        c.putheader("Connection", "Upgrade")
        c.putheader("Upgrade", "tcp")
    c.endheaders()
    c.send(payload)
    if stdin_data is not None:
        # read upgrade response headers directly off socket
        resp = c.sock.recv(4096)
        c.sock.sendall(stdin_data)
        c.sock.shutdown(socket.SHUT_WR)
        chunks = []
        while True:
            b = c.sock.recv(65536)
            if not b:
                break
            chunks.append(b)
        raw_out = b"".join(chunks)
    else:
        r = c.getresponse()
        raw_out = r.read()
    c.close()
    sys.stdout.write(demux(raw_out).decode(errors="replace"))
    st2, insp = req("GET", f"/exec/{eid}/json")
    code = json.loads(insp).get("ExitCode")
    sys.exit(code if isinstance(code, int) else 0)

if __name__ == "__main__":
    op = sys.argv[1]
    if op == "exec":
        # dk.py exec <container> <cmd...>  (reads stdin if not a tty)
        stdin_data = None if sys.stdin.isatty() else sys.stdin.buffer.read()
        if stdin_data == b"":
            stdin_data = None
        dexec(sys.argv[2], sys.argv[3:], stdin_data)
    elif op == "api":
        # dk.py api <METHOD> <path> [json-body]
        body = json.loads(sys.argv[4]) if len(sys.argv) > 4 else None
        st, raw = req(sys.argv[2], sys.argv[3], body)
        print(st)
        sys.stdout.write(raw.decode(errors="replace"))
    elif op == "logs":
        st, raw = req("GET", f"/containers/{sys.argv[2]}/logs?stdout=true&stderr=true&tail={sys.argv[3] if len(sys.argv)>3 else 100}")
        sys.stdout.write(demux(raw).decode(errors="replace"))
