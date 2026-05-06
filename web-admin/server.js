const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const MIME = { '.html':'text/html','.js':'application/javascript','.css':'text/css','.json':'application/json' };

http.createServer((req, res) => {
  const filePath = req.url === '/' ? '/index.html' : req.url;
  const ext = path.extname(filePath);
  try {
    const content = fs.readFileSync(__dirname + filePath);
    res.writeHead(200, { 'Content-Type': MIME[ext] || 'text/plain' });
    res.end(content);
  } catch {
    res.writeHead(404);
    res.end('Not Found');
  }
}).listen(PORT, () => {
  console.log(`Hawkeye Admin: http://localhost:${PORT}`);
});
