const http = require('http');

const data = JSON.stringify({
  id: 1,
  currentPassword: "OldPassword1!",
  password: "weak"
});

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/user/update-password',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

const req = http.request(options, (res) => {
  console.log(`STATUS: ${res.statusCode}`);
  res.on('data', (chunk) => {
    console.log(`BODY: ${chunk}`);
  });
});

req.on('error', (e) => {
  console.error(`problem with request: ${e.message}`);
});

req.write(data);
req.end();
