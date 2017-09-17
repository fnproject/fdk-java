var request = require('request');

var api_url = process.env.CAR_API_URL;

request.post(
    api_url,
    { json: { model: 'Tesla Model S P100D' } },
    function (error, response, body) {
        if (!error && response.statusCode == 200) {
            console.log(JSON.stringify(response.body));
        }
    }
);
