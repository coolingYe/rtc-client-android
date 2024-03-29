# Grammar

Here the syntax of the JSON object generated by `sdptransform::parse(sdp)`. Each section below defines how the corresponding SDP line is parsed.

Some lines are parsed as a JSON object with different keys, while others have a JSON string, JSON integer or JSON float as value.

* The name of each subsection below corresponds to the `key` string in the SDP JSON object (or the corresponding `media` section).
* The `multiple` column means that, if at least one of those lines exists in the SDP, its value is a JSON array with multiple values (for eample, the `a=rtpmap` line).
* `type` indicates the C++ valid conversion for the value.

**IMPORTANT:**

Some fields in a SDP line may be optional, and others may be mandatory but could have an empty string as valid value. In both cases (not present fields and fields with an empty string as value) the corresponding `key` is not inserted in the parsed JSON.

This means that, before assuming that a JSON key exists, the presence of the key must be verified:

```c++
int sessionId;

if (
  session.find("origin") != session.end() &&
  session.at("origin").find("sessionId") != session.at("origin").end()
)
{
  sessionId = session.at("origin").at("sessionId");
}
```


### version

`v=0`

* type: string 
* example: "0"


### origin

`o=- 20518 0 IN IP4 203.0.113.1`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| username        | string  | "-"
| sessionId       | integer | 20518
| sessionVersion  | integer | 0
| netType         | string  | "IN"
| ipVer           | integer | 4
| adddress        | string  | "203.0.113.1"
`s=-`


### description

`i=foo`

* type: string 
* example: "foo"


### uri

`u=https://foo.com`

* type: string 
* example: "https://foo.com"


### email

`e=alice@foo.com`

* type: string 
* example: "alice@foo.com"


### phone

`p=+12345678`

* type: string 
* example: "+12345678"


### timing

`t=0 0`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| start           | integer | 0
| stop            | integer | 0


### connection

`c=IN IP4 10.47.197.26`

`c=IN IP4 224.2.36.42/15`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| version         | integer | 4
| ip              | string  | "10.47.197.26"
| ttl             | integer | 15

*NOTE:* `ttl` is just present in the object if `ip` is followed by `/` plus a number. More info in the [RFC 4566 section 5.7](https://tools.ietf.org/html/rfc4566#section-5.7).

### bandwidth

`b=AS:4000`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| type            | string  | "AS"
| limit           | integer | 4000


### media

`m=video 51744 RTP/AVP 126 97 98 34 31`

`m=audio 5004/2 RTP/AVP 96`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| type            | string  | "video"
| port            | integer | 51744
| numPorts        | integer | 2 (optional)
| protocol        | string  | "RTP/AVP"
| payloads        | string  | "126 97 98 34 31"

*NOTE:* `numPorts` is just present in the object if `port` is followed by `/` plus a number. More info in the [RFC 4566 section 5.14](https://tools.ietf.org/html/rfc4566#section-5.14).


### rtp

`a=rtpmap:110 opus/48000/2`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| payload         | integer | 110
| codec           | string  | "opus"
| rate            | integer | 48000
| encoding        | string   "2"


### fmtp

`a=fmtp:108 profile-level-id=24;bitrate=64000`

`a=fmtp:111 minptime=10; useinbandfec=1`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| payload         | integer | 108
| config          | string  | "profile-level-id=24;bitrate=64000"


### control

`a=control:streamid=0`

* type: string 
* example: "streamid=0"


### rtcp

`a=rtcp:65179 IN IP4 193.84.77.194`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| port            | integer | 65179
| netType         | string  | "IN"
| ipVer           | integer | 4
| adddress        | string  | "193.84.77.194"


### rtcpFbTrrInt

`a=rtcp-fb:98 trr-int 100`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| payload         | string  | "98" (could be "*")
| value           | integer | 100


### rtcpFb

`a=rtcp-fb:98 nack rpsi`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| payload         | string  | "98" (could be "*")
| type            | string  | "nack"
| subtype         | string  | "rpsi"


### ext

`a=extmap:1/recvonly URI-gps-string`

`a=extmap:2 urn:ietf:params:rtp-hdrext:toffset`

`a=extmap:3 urn:ietf:params:rtp-hdrext:encrypt urn:ietf:params:rtp-hdrext:smpte-tc 25@600/24`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| value           | integer | 1
| direction       | string  | "recvonly"
| uri             | string  | "URI-gps-string"
| encrypt-uri     | string  | "urn:ietf:params:rtp-hdrext:encrypt"
| config          | string  |


### crypto

`a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:PS1uQCVeeCFCanVmcjkpPywjNWhcYD0mXXtxaVBR|2^20|1:32`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| id              | integer | 1
| suite           | string  | "AES_CM_128_HMAC_SHA1_80"
| config          | string  | "inline:PS1uQCVeeCFCanVmcjkpPywjNWhcYD0mXXtxaVBR\|2^20\|1:32"
| sessionConfig   | string  |


### setup

`a=setup:actpass`

* type: string 
* example: "actpass"


### mid

`a=mid:audio`

* type: string 
* example: "audio"


### msid

`a=msid:0c8b064d-d807-43b4 46e0-8e16-7ef0db0db64a`

* type: string 
* example: "0c8b064d-d807-43b4 46e0-8e16-7ef0db0db64a"


### ptime

`a=ptime:20`

* type: integer 
* example: 20


### maxptime

`a=maxptime:60`

* type: integer 
* example: 60


### direction

`a=sendrecv`

* type: string 
* example: "sendrecv"


### icelite

`a=ice-lite`

* type: string 
* example: "ice-lite"


### iceUfrag

`a=ice-ufrag:F7gI`

* type: string 
* example: "F7gI"


### icePwd

`a=ice-pwd:x9cml/YzichV2+XlhiMu8g`

* type: string 
* example: "x9cml/YzichV2+XlhiMu8g"


### fingerprint

`a=fingerprint:SHA-1 00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| type            | string  | "SHA-1"
| hash            | string  | "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33"


### candidates

`a=candidate:0 1 UDP 2113667327 203.0.113.1 54400 typ host`

`a=candidate:1162875081 1 udp 2113937151 192.168.34.75 60017 typ host generation 0 network-id 3 network-cost 10`

`a=candidate:3289912957 2 udp 1845501695 193.84.77.194 60017 typ srflx raddr 192.168.34.75 rport 60017 generation 0 network-id 3 network-cost 10`

`a=candidate:229815620 1 tcp 1518280447 192.168.150.19 60017 typ host tcptype active generation 0 network-id 3 network-cost 10`

`a=candidate:3289912957 2 tcp 1845501695 193.84.77.194 60017 typ srflx raddr 192.168.34.75 rport 60017 tcptype passive generation 0 network-id 3 network-cost 10`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| foundation      | string  | "3289912957"
| component       | integer | 2
| transport       | string  | "tcp"
| priority        | integer | 1845501695
| ip              | string  | "193.84.77.194"
| port            | integer | 60017
| type            | string  | "srflx"
| raddr           | string  | "192.168.34.75"
| rport           | integer | 60017
| generation      | integer | 0
| network-id      | integer | 3
| network-cost    | integer | 10


### endOfCandidates

`a=end-of-candidates`

* type: string 
* example: "end-of-candidates"


### remoteCandidates

`a=remote-candidates:1 203.0.113.1 54400 2 203.0.113.1 54401`

* type: string 
* example: "1 203.0.113.1 54400 2 203.0.113.1 54401"


### iceOptions

`a=ice-options:google-ice`

* type: string 
* example: "google-ice"


### ssrcs

`a=ssrc:2566107569 cname:t9YU8M1UxTF8Y1A1`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| id              | integer | 2566107569
| attribute       | string  | "cname"
| value           | string  | "t9YU8M1UxTF8Y1A1"


### ssrcGroups

`a=ssrc-group:FEC-FR 3004364195 1080772241`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| semantics       | string  | "FEC-FR"
| ssrcs           | string  | "3004364195 1080772241"


### msidSemantic

`a=msid-semantic: WMS Jvlam5X3SX1OP6pn20zWogvaKJz5Hjf9OnlV`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| semantic        | string  | "WMS"
| token           | string  | "Jvlam5X3SX1OP6pn20zWogvaKJz5Hjf9OnlV"


### groups

`a=group:BUNDLE audio video`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| type            | string  | "BUNDLE"
| mids            | string  | "audio video"


### rtcpMux

`a=rtcp-mux`

* type: string 
* example: "rtcp-mux"


### rtcpRsize

`a=rtcp-rsize`

* type: string 
* example: "rtcp-rsize"


### sctpmap

`a=sctpmap:5000 webrtc-datachannel 1024`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| sctpmapNumber   | integer | 5000
| app             | string  | "webrtc-datachannel"
| maxMessageSize  | integer | 1024


### xGoogleFlag

`a=x-google-flag:conference`

* type: string 
* example: "conference"


### rids

`a=rid:1 send max-width=1280;max-height=720`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| id              | string  | "1"
| direction       | string  | "send"
| params          | string  | "max-width=1280;max-height=720"


### imageattrs

`a=imageattr:97 send [x=800,y=640,sar=1.1,q=0.6] [x=480,y=320] recv [x=330,y=250]`

`a=imageattr:* send [x=800,y=640] recv *`

`a=imageattr:100 recv [x=320,y=240]`

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| pt              | string  | "97" (could be "*")
| dir1            | string  | "send"
| attrs1          | string  | "[x=800,y=640,sar=1.1,q=0.6] [x=480,y=320]"
| dir2            | string  | "recv"
| attrs2          | string  | "[x=330,y=250]"


### simulcast

`a=simulcast:send 1,2,3;~4,~5 recv 6;~7,~8`

`a=simulcast:recv 1;4,5 send 6;7`

* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| dir1            | string  | "send"
| list1           | string  | "1,2,3;~4,~5"
| dir2            | string  | "recv"
| list2           | string  | "6;~7,~8"


### simulcast_03

Old simulcast draft [revision 03](https://tools.ietf.org/html/draft-ietf-mmusic-sdp-simulcast-03) (implemented by some browsers).

`a=simulcast: recv pt=97;98 send pt=97`

`a=simulcast: send rid=5;6;7 paused=6,7`

* type: string
* example: "recv pt=97;98 send pt=97"


### framerate

`a=framerate:25`

`a=framerate:29.97`

* type: float
* example: 25.0


### tsRefclk

`a=ts-refclk:ptp=IEEE1588-2008:00-50-C2-FF-FE-90-04-37:0`

* type: string
* example: "ptp=IEEE1588-2008:00-1D-C1-FF-FE-12-00-A4:0"


### mediaclk

`a=mediaclk:direct=0`

`a=mediaclk:sender`

* type: string
* example: "direct=0"


### invalid

Unknown SDP lines are stored within the `invalid` key.

* multiple
* type: object

| field           | type    | example
| --------------- | ------- | -------------------------
| value           | string  |
