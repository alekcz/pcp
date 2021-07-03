// requires k6.
// install using: 
// $ brew install k6
// $ k6 run loadtest.js


import http from "k6/http";

export let options = {
   stages: [
    { duration: '10s', target: 200 }, 
    { duration: '05s', target: 300 },
    { duration: '05s', target: 400 },
    { duration: '05m', target: 400 }, 
    { duration: '05s', target: 400 },
    { duration: '05s', target: 300 }, 
    { duration: '10s', target: 200 },
    { duration: '10s', target: 0 },
  ],
};
export default function() {
    let response = http.get("http://localhost:3000");
};
