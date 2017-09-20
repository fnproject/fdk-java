var request = require('request');

var api_url = process.env.CAR_API_URL;

request.delete(
    api_url,

    function (error, response, body) {
        if (!error && response.statusCode == 200) {
            console.log(response.body);
        } else {
            throw new Error();
        }
    }
);
