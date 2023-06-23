#!/usr/bin/env node
// Test app to check what happens with event streams.
// serves a form that has a textarea where one can input testdata to be returned from an event stream.
// posting that form saves that data and then an eventstream request is triggered that requests that data, and
// the result is displayed below that form.
const http = require('node:http');
const url = require('node:url');
const server = http.createServer(processRequest);
const port = 7665;
server.listen(port, () => {
    console.log(`Server running at http://localhost:${port}/`);
});

var eventdata;

/**
 * Processes a request.
 * @param {http.IncomingMessage} req
 * @param {http.ServerResponse} res
 */
function processRequest(req, res) {
    console.log(`Request: ${req.method} ${req.url}`);

    if (req.method === 'OPTIONS') {
        res.setHeader('Allow', '*');
        res.writeHead(200);
        res.end();
        return;
    }

    res.setHeader('Cache-Control', 'no-store');

    const reqUrl = url.parse(req.url, true);

    switch (reqUrl.pathname) {
        case '/':
            res.setHeader('Content-Type', 'text/html;charset=utf-8');
            res.writeHead(200, 'OK');
            res.end(formHtml);
            break;
        case '/saveevents':
            if (req.method !== 'POST') {
                res.writeHead(405, 'Method Not Allowed');
                return res.end();
            }
            // read text into eventdata
            eventdata = '';
            req.on('data', chunk => {
                eventdata += chunk.toString();
            });
            req.on('end', () => {
                console.log('Event data saved: ', eventdata);
                res.writeHead(204, 'Saved');
                res.end();
            });

            break;
        case '/events':
            if (req.method !== 'GET') {
                res.writeHead(405, 'Method Not Allowed');
                return res.end();
            }
            res.setHeader('Content-Type', 'text/event-stream;charset=utf-8');
            res.writeHead(200, 'OK');
            res.end(eventdata);
            break;
        default:
            res.writeHead(404, 'Not Found');
            res.end();
            break;
    }
}

var formHtml = `
`;
