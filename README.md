#sse-perf  

I wanted to know how many concurrent Server Sent Event streams my application could handle but I did not find the right tool for this, so I wrote one myself. This web app starts many WS connections with a specified address and measures the amount of data and the number of individual events coming in through all the connections combined.

So far the maximum number of concurrent connections I established from this application to another server is 2,700, only because the remote node could not serve more connections in a timely fashion. This app probably would have established a lot more connections.
 
 Creating additional connections is done in increments of up to 50 additional clients at a time. I have found that doing more at a time more results in a few inactive clients, while up to 50 at a time work fine.  

###Setup
There is not much to the setup, given that you have a working installation of Play on your computer. All you need to do is **play run** in your shell, or **play "run 9001"** for example if you are already using port 9000 for the application you want to test. The you need to open **http://localhost:9001**, or whatever port you chose for this application.

## Licence

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Copyright &copy; 2013 **[Matthias Nehlsen](http://www.matthiasnehlsen.com)**.
