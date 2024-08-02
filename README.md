# Scala Play RESTful API

`Enter a brief description here`

## Running the project

`Enter instructions on how to run your application here`

## Technologies

* `Enter a list of technologies used within the project`

## Languages

`Add information about languages used`

## Features

* `Details of main features`
* `Details of any special features`

## Tests

`Add information about the testing framwork`

---

## License

MIT License

Copyright (c) [2024]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files, to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

Notes: 

Add notes on CURL with terminal such as:
-X PUT: Specifies that the HTTP method to be used is PUT.
-H "Content-Type: application/json": Sets the content type of the request to JSON.
-d '{...}': Provides the JSON data to be sent with the request.
"http://localhost:9000/api/update/1": The URL to which the PUT request is sent (assuming your endpoint for updating resources uses this pattern).

curl -X DELETE -H "Content-Type: application/json" -d '{ "_id" : "1", "name" : "testNameUpdate", "description" : "testDescription", "pageCount" : 1 }' "http://localhost:9000/api/1" -i
curl -X DELETE "localhost:9000/api/2"

curl "localhost:9000/api" -i


understand more about injecting dependencies? 
Why @Singleton etc?
What is EitherT

Update all the controller methods to use EitherT and return database errors, then wiremock in order to test for them. 


Read optional params:
/api/read?id=someIdValue
/api/read?name=someNameValue