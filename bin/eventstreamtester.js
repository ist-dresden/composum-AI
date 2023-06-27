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
            if (!eventdata) {
                res.writeHead(410, 'No data anymore');
                return res.end();
            }
            res.setHeader('Content-Type', 'text/event-stream;charset=utf-8');
            res.writeHead(200, 'OK');
            res.end(eventdata);
            eventdata = undefined;
            break;
        default:
            res.writeHead(404, 'Not Found');
            res.end();
            break;
    }
}

var formHtml = `
<!DOCTYPE html>
<html>
<head>
    <title>Event Stream Tester</title>
</head>
<body>
    <form onsubmit="submitForm(event)">
        <textarea id="testdata" rows="15" cols="120"></textarea>
        <br/>
        <button type="submit">Send</button>
    </form>
    <pre id="result"></pre>
    <script>
        function submitForm(event) {
            event.preventDefault();
            document.getElementById('result').textContent = '';
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/saveevents', true);
            xhr.onload = function () {
                if (xhr.status === 204) {
                    var eventSource = new EventSource('/events');
                    eventSource.addEventListener('finished', onMyEvent);
                    eventSource.onmessage = onMessage;
                    eventSource.onerror = onError;
                    setTimeout(() => eventSource.close(), 10000);
                }
            };
            xhr.send(document.getElementById('testdata').value);
        }
        
        function onMessage(event) {
            console.log('onMessage', arguments);
            const newEvent = '\\n=== message ' + event.type + '\\n' + event.data;
            document.getElementById('result').textContent += newEvent;
        }
        
        function onError(event) {
            console.log('onError', arguments);
            const newEvent = '\\n=== ERROR ' + event.type + '\\n' + event.data + '\\n' + JSON.stringify(event);
            document.getElementById('result').textContent += newEvent;
        }
        
        function onMyEvent(event) {
            console.log('onMyEvent', arguments);
            const newEvent = '\\n=== myevent ' + event.type + '\\n' + event.data;
            document.getElementById('result').textContent += newEvent;
        }
        
        // set testdata to some default value
        document.getElementById('testdata').value = 'data: "Bäume sind in der Höhle."\\n\\n' + 
        'event: finished\\n' + 
        'data: {"status":200,"success":true,"warning":false,"data":{"result":{"finishreason":"STOP"}}}\\n\\n';
    </script>
</body>
</html>
`;
