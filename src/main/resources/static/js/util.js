/**
 * MIT License
 *
 * Copyright (c) 2021 Infineon Technologies AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

const serverurl = 'https://' + location.host;
const not_secure_serverurl = 'http://' + location.host;
const RESP_OK = 'ok';
const RESP_ERR = 'ko';
const token = $("meta[name='_csrf']").attr('content');
const headerName = $("meta[name='_csrf_header']").attr('content'); // X-CSRF-TOKEN

function fFormToJSON( form ) {
  var obj = {};
  var elements = form.querySelectorAll( "input, select, textarea" );
  for( var i = 0; i < elements.length; ++i ) {
    var element = elements[i];
    var name = element.name;
    var value = element.value;

    if( name ) {
      obj[ name ] = value;
    }
  }
  return obj;
}

/* Synchronous request has been deprecated, avoid using it */
function fWebApiSync(mode, api, txJson, fCallback) {
  fBaseWebApi(mode, serverurl + '/' + api, txJson, fCallback, false);
}

/* Synchronous request has been deprecated, avoid using it */
function fXSWebApiSync(mode, url, txJson, fCallback) {
  fBaseWebApi(mode, url, txJson, fCallback, false);
}

function fWebApi(mode, api, txJson, fCallback) {
  fBaseWebApi(mode, serverurl + '/' + api, txJson, fCallback, true);
}

function fXSWebApi(mode, url, txJson, fCallback) {
  fBaseWebApi(mode, url, txJson, fCallback, true);
}
function fBaseWebApi(mode, url, txJson, fCallback, isAsync) {
  $.ajax({
    url: url,
    method: mode,
    async: isAsync,
    headers: {
      'X-CSRF-TOKEN': token,
    },
    contentType: 'application/json',
    dataType: 'json',
    data: txJson,
  }).done(function(data, textStatus, xhr) {
    console.log('Server Responded:' + JSON.stringify(data));
    fCallback(xhr.status, data);
  }).fail(function(xhr, textStatus, errorThrown) {
    let json = null
    try {
      json = JSON.parse(xhr.responseText);
    } catch (e) {}
    console.log('Server Responded:' + json);
    fCallback(xhr.status, json);
  });
}

function fWebSocket_connect(fCallback) {
  var socket = new SockJS('/websocket');
  stompClient = Stomp.over(socket);
  stompClient.debug = null; // hide all console messages
  var headers = {};
  headers[headerName] = token;
  stompClient.connect(headers, fCallback);
  return stompClient;
}

function fCheckBrowser() {
  var ua = window.navigator.userAgent;
  var msie = ua.indexOf("MSIE ");

  if (msie > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./))  // If Internet Explorer, return version number
  {
    alert('Not compatible with Internet Explorer, use Microsoft Edge or other browsers');
  }
}
