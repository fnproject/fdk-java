require 'net/http'
require 'uri'
require 'json'

uri = URI.parse(ENV["HOTEL_API_URL"])

header = {'Content-Type': 'application/json'}

args = JSON.parse(ARGF.read)

hotel_request = {
  city: args["city"],
  hotel: args["hotel"]
}

http = Net::HTTP.new(uri.host, uri.port)
request = Net::HTTP::Delete.new(uri.request_uri, header)
response = http.request(request)

if response.kind_of? Net::HTTPSuccess
  puts response.body
else
  raise
end