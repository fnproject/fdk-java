var request = require('request');
var fs = require('fs');

var api_url = process.env.CAR_API_URL;
var stdin = JSON.parse(fs.readFileSync('/dev/stdin').toString());

request.post(
    api_url,
    { json: stdin },
    function (error, response, body) {
        if (!error && response.statusCode === 200) {
            console.log(JSON.stringify(response.body));
        } else {
            throw new Error();
        }
    }
);
