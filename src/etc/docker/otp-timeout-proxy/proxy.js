const http = require('node:http');

// No dependencies; only the test control listener is bound to loopback.
let fault = { target: null, hits: 0 };
const timers = new Set();
http.createServer((req, res) => {
  if (req.method === 'POST' && req.url === '/fault') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const { target } = JSON.parse(body);
        if (![null, 'verification', 'user'].includes(target)) throw new Error('Invalid target');
        fault = { target, hits: 0 };
        res.writeHead(200).end();
      } catch {
        res.writeHead(400).end();
      }
    });
  } else if (req.method === 'GET' && req.url === '/fault') {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(fault));
  } else {
    res.writeHead(404).end();
  }
}).listen(3002, '127.0.0.1');

http.createServer((req, res) => {
  const target = req.method === 'POST' && req.url.startsWith('/protected/users/')
    ? 'verification' : req.method === 'GET' && req.url.startsWith('/users/') ? 'user' : null;
  const forward = () => {
    if (res.destroyed) return;
    const upstream = http.request({
      hostname: '127.0.0.1', port: 3000, path: req.url, method: req.method,
      headers: { ...req.headers, host: 'localhost:3000' },
    }, response => {
      res.writeHead(response.statusCode, response.headers);
      response.pipe(res);
    });
    upstream.on('error', () => { if (!res.destroyed) res.writeHead(502).end(); });
    res.on('close', () => upstream.destroy());
    req.pipe(upstream);
  };
  // Browser fetches include Origin/Sec-Fetch-Mode; faults must affect CAS only.
  const serverRequest = !req.headers.origin && !req.headers['sec-fetch-mode'];
  if (serverRequest && target && target === fault.target && fault.hits === 0) {
    fault.hits++;
    // CAS must time out before a successful response can reach it. Consume only
    // one fault so the browser's subsequent API calls and a retry work normally.
    const timer = setTimeout(() => { timers.delete(timer); forward(); }, 3000);
    timers.add(timer);
    res.on('close', () => { clearTimeout(timer); timers.delete(timer); });
  } else {
    forward();
  }
}).listen(3001, '0.0.0.0');
