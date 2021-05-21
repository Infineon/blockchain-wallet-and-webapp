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

window.onload = function() {
  console.log('website loaded');
  console.log(user);
  console.log(accounts);
  fCheckBrowser();
  fTableInit();
  fWebSocket_init();
  fGetUsername();
}

function fGetUsername() {
  fWebApi('GET', 'get-username', null,function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      console.log('logged in as: ' + json.data);
    } else {
      console.log('failed to fetch username');
    }
  });
}

function fSignOut() {
  fWebApi('GET', 'signout', null,function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      location.href = '/entry';
    }
  });
}

function fWebSocket_init() {
  let stompClient = fWebSocket_connect( function(frame) {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/user/topic/private', function (messageOutput) {
      console.log(messageOutput.body);
      fRx(messageOutput.body);
    });
  });
}

function fTableInit() {
  if (accounts != null)
    accounts.forEach(function(item, index) {
      let convert = new BigNumber(EthJS.util.stripHexPrefix(item.balance), 16).toString(10);
      fTableAddRow(item.address, EthJS.units.convert(convert, 'wei', 'eth'));
      fDropdownAddOption(item.address);
    });
}

function fTableAddRow(address, balance) {
  $("#id-tab-head-acc-table tr").filter(function() {
    let this_row_2nd_col = $(this).children(':eq(1)').text();
    return this_row_2nd_col == address;
  }).remove();
  let html =
      "<tr id='" + address + "' style='word-break: break-word;'>" +
        "<td><input type='checkbox' name='tr-checkbox'></td>" +
        "<td><a href='https://ropsten.etherscan.io/address/" + address + "'>" + address + "</a></td>" +
        "<td>" + balance + "</td>" +
      "</tr>";
  $("#id-tab-head-acc-table").append(html);
}

function fTableRemoveRow(address) {
  $('#' + address).remove();
}

function fTableRefreshRow(address, balance) {
  fTableAddRow(address, balance);
}

function fDropdownAddOption(address) {
  $("#id-tab-body-trans-form-from option").filter(function() {
    let option = $(this).text();
    return option == address;
  }).remove();
  let html = "<option id='" + address + "'>" + address + "</option>";
  $("#id-tab-body-trans-form-from").append(html);

  $("#id-tab-body-topup-form-accounts option").filter(function() {
    let option = $(this).text();
    return option == address;
  }).remove();
  html = "<option id='" + address + "'>" + address + "</option>";
  $("#id-tab-body-topup-form-accounts").append(html);
}

function fDropdownRemoveOption(address) {
  $('#' + address).remove();
}

$('#id-tab-head-acc-btn-refresh').click(function(){
  $('#id-tab-head-acc-btn-refresh').prop('disabled', true);
  $('#id-tab-head-acc-btn-refresh').html("<span class='spinner-border spinner-border-sm'></span> Loading...");
  fWebApi('GET', 'account-refresh', null, function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      if (json.data !== null)
        for (let i in json.data.accounts) {
          let account = json.data.accounts[i];
          let address = account.address;
          let balance = account.balance;
          let convert = new BigNumber(EthJS.util.stripHexPrefix(balance), 16).toString(10);
          let eth = EthJS.units.convert(convert, 'wei', 'eth');
          fTableAddRow(address, eth);
        }
    } else {
      alert('Unable to fetch latest balance!');
    }
    $('#id-tab-head-acc-btn-refresh').prop('disabled', false);
    $('#id-tab-head-acc-btn-refresh').html("<i class='fas fa-sync'></i> Refresh");
  });
});

$('#id-tab-head-acc-btn-link').click(function(){
  fClearAccLinkQR();
  $('#id-tab-head-acc-btn-link').prop('disabled', true);
  $('#id-tab-head-acc-btn-link').html("<span class='spinner-border spinner-border-sm'></span> Loading...");
  fWebApi('GET', 'account-add-hw-req', null, function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      let qr = {
        action: 'register',
        url: 'https://' + location.host + '/account-add-hw',
        token: json.data.token,
      };
      fCreateAccLinkQR(JSON.stringify(qr));
    } else {
      alert('Hardware wallet link request failed!');
    }
    $('#id-tab-head-acc-btn-link').prop('disabled', false);
    $('#id-tab-head-acc-btn-link').html('Link Hardware Wallet');
  });
});

$('#id-tab-head-acc-btn-rm').click(function(){
  let addrList = [];
  $('#id-tab-head-acc-table').find("input[name='tr-checkbox']").each(function () {
    if ($(this).is(":checked")) {
      let tr = $(this).parents('tr');
      let address = tr.find('td:eq(1)').text();
      addrList.push(address);
    }
  });
  let btnTxt = $('#id-tab-head-acc-btn-rm').text();
  $('#id-tab-head-acc-btn-rm').prop('disabled', true);
  $('#id-tab-head-acc-btn-rm').html("<span class='spinner-border spinner-border-sm'></span> Loading...");
  let recursive = function(index) {
    if (index < addrList.length) {
      fWebApi('POST', 'account-remove', JSON.stringify({address: addrList[index]}), function (status, json) {
        if (status === 200 && json.status === RESP_OK) {
          fTableRemoveRow(addrList[index]);
          fDropdownRemoveOption(addrList[index]);
        }
        recursive(++index);
      });
    } else {
      $('#id-tab-head-acc-btn-rm').prop('disabled', false);
      $('#id-tab-head-acc-btn-rm').html(btnTxt);
    }
  };
  recursive(0);
});

function fLink() {
  let form = $('#id-tab-head-acc-form')[0];
  let formJson = fFormToJSON( form );
  let btnTxt = $('#id-tab-head-acc-form-btn').text();
  $('#id-tab-head-acc-form-btn').prop('disabled', true);
  $('#id-tab-head-acc-form-btn').html("<span class='spinner-border spinner-border-sm'></span> Querying blockchain for account balance...");
  fWebApi('POST', 'account-add', JSON.stringify({address:formJson.address}), function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      fUpdateAccountList(json.data.address, json.data.balance);
    } else {
      alert('Unexpected failure during account registration!');
    }
    $('#id-tab-head-acc-form-btn').prop('disabled', false);
    $('#id-tab-head-acc-form-btn').html(btnTxt);
  });
}

async function fTransact() {
  fClearTransactionQR();
  let form = $('#id-tab-body-trans-form')[0];
  let formJson = fFormToJSON( form );
  let from = formJson.from;
  let to = formJson.to;
  $('#id-tab-body-trans-form-btn').prop('disabled', true);
  $('#id-tab-body-trans-form-btn').html("<span class='spinner-border spinner-border-sm'></span> Querying blockchain for latest account information...");
  fWebApi('POST', 'account-info', JSON.stringify({address:from}), function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      let bigNumToTransfer = new BigNumber(EthJS.units.convert(formJson.value, 'eth', 'wei'), 10);
      let bigNumGasPrice = new BigNumber(EthJS.util.stripHexPrefix(json.data.gasPrice), 16);
      let bigNumBalance = new BigNumber(EthJS.util.stripHexPrefix(json.data.balance), 16);
      let bigNumEstimatedGas = new BigNumber(EthJS.util.stripHexPrefix(json.data.estimatedGas), 16);
      let bigNumFee = bigNumGasPrice.times(bigNumEstimatedGas);
      let bigNumTotalDeduct = bigNumFee.plus(bigNumToTransfer);
      let bigNumMaxCanTransfer =  bigNumBalance.minus(bigNumFee);
      let convertBalance = bigNumBalance.toString(10);
      let gasPriceEth = EthJS.units.convert(bigNumGasPrice.toString(10), 'wei', 'eth');
      let balanceEth = EthJS.units.convert(convertBalance, 'wei', 'eth');
      let maxCanTransferWei = bigNumMaxCanTransfer.toString(10);
      let maxCanTransferEth = EthJS.units.convert(maxCanTransferWei, 'wei', 'eth');
      let transferWei = bigNumToTransfer.toString(10);
      let transferEth = EthJS.units.convert(transferWei, 'wei', 'eth');
      let feeWei = bigNumFee.toString(10);
      let feeEth = EthJS.units.convert(feeWei, 'wei', 'eth');
      $('#id-tab-body-trans-form-gas-price').val(gasPriceEth);
      $('#id-tab-body-trans-form-gas-estimated').val(bigNumEstimatedGas.toString(10));
      $('#id-tab-body-trans-form-fee').val(feeEth);
      fTableRefreshRow(from, balanceEth);

      if (new BigNumber(formJson.gasLimit, 10).lessThan(bigNumEstimatedGas)) {
        alert('gasLimit is less than estimated amount: ' + bigNumEstimatedGas.toString(10));
        $('#id-tab-body-trans-form-btn').prop('disabled', false);
        $('#id-tab-body-trans-form-btn').html('Transact');
      } else if (bigNumBalance.lessThan(bigNumTotalDeduct)) {
        alert('Transfer amount ' + transferEth + ' Ether + estimated transaction fee ' +
            feeEth + ' Ether exceeded available balance of ' + balanceEth + ' Ether\n' +
            'Maximum allowable transfer is ' + maxCanTransferEth + ' Ether');
        $('#id-tab-body-trans-form-btn').prop('disabled', false);
        $('#id-tab-body-trans-form-btn').html('Transact');
      } else {
        let gasLimit = '0x' + new BigNumber(formJson.gasLimit, 10).toString(16);
        let toTransfer = '0x' + bigNumToTransfer.toString(16);
        fCraftRawTransaction(from, to, json.data.transactionCount, toTransfer,
            json.data.gasPrice, gasLimit, bigNumEstimatedGas.toString(10));
      }
    } else {
      alert('Unable to fetch account information!');
      $('#id-tab-body-trans-form-btn').prop('disabled', false);
      $('#id-tab-body-trans-form-btn').html('Transact');
    }
  });

}

function fCraftRawTransaction(from, to, nonce, amount, gasPrice, gasLimit, estimatedGas) {
  let txData = {
    nonce: nonce,
    gasPrice: gasPrice,
    gasLimit: gasLimit,
    to: to,
    value: amount,
  };
  let tx = new EthJS.tx.Transaction(txData, { chain: 'ropsten' });
  let serialized = tx.serialize();
  let hash = tx.hash(false);
  let req = txData;
  req.from = from;
  req.hash = EthJS.util.bufferToHex(hash);
  req.serialized = EthJS.util.bufferToHex(serialized);

  $('#id-tab-body-trans-form-btn').html("<span class='spinner-border spinner-border-sm'></span> Audit generated transaction...");
  fWebApi('POST', 'account-transact', JSON.stringify(req), function (status, json) {
    if (status === 200 && json.status === RESP_OK) {
      let qr = {
        action: 'sign',
        url: 'https://' + location.host + '/account-transact-sign',
        token: json.data.token,
        signer: json.data.signerAddr,
        serialized: req.serialized,
        gasEstimation: estimatedGas,
      };
      fCreateTransactionQR(JSON.stringify(qr));
    } else {
      alert('Unable to register raw transaction!');
    }
    $('#id-tab-body-trans-form-btn').prop('disabled', false);
    $('#id-tab-body-trans-form-btn').html('Transact');
  });

  //
}

async function fTopup() {
  let address = $('#id-tab-body-topup-form-accounts').find(':selected').text();

  $('#id-tab-body-topup-form-btn').prop('disabled', true);
  $('#id-tab-body-topup-form-btn').html("<span class='spinner-border spinner-border-sm'></span> Requesting...");
  fXSWebApi('GET', "https://faucet.ropsten.be/donate/" + address, null, function (status, rxJson) {
    if (status === 200 && status === 200) {
      let eth = EthJS.units.convert(rxJson.amount, 'wei', 'eth');
      if (rxJson.txhash !== undefined) {
        fTopupOKNotification(
            "Transaction is under way, check the transaction status <a href='https://ropsten.etherscan.io/tx/" +
            rxJson.txhash +
            "'>here</a>. You should receive " +
            eth +
            " Ether."
        );
      } else {
        fTopupOKNotification(
            "Transaction is under way, check the transaction status <a href='https://ropsten.etherscan.io/address/" +
            rxJson.address +
            "'>here</a>. You should receive " +
            eth +
            " Ether."
        );
      }
    } else if (status === 400) {
      fTopupKONotification('Unable to topup, received error from the faucet: The address is invalid.');
    } else if (status === 403) {
      fTopupKONotification('Unable to topup, received error from the faucet: The queue is full / you are greylisted for requesting more than once every 24 hours / blacklisted.');
    } else if (status === 404) {
      fTopupKONotification('Unable to topup, faucet api is not found.');
    } else if (status === 500) {
      fTopupKONotification('Unable to topup, received error from the faucet: Internal faucet error.');
    } else {
      fTopupKONotification('Unable to topup, unknown error.');
    }
    $('#id-tab-body-topup-form-btn').prop('disabled', false);
    $('#id-tab-body-topup-form-btn').html('Topup');
  });
}

function fTopupKONotification(message) {
  $('#id-tab-body-topup-form-notify').html(
      "<div class='alert alert-danger alert-dismissable mt-3'>" +
      "  <button type='button' class='close' data-dismiss='alert' aria-hidden='true'>&times;</button>" +
         message +
      "</div>"
  );
}

function fTopupOKNotification(message) {
  $('#id-tab-body-topup-form-notify').html(
      "<div class='alert alert-success alert-dismissable mt-3'>" +
      "  <button type='button' class='close' data-dismiss='alert' aria-hidden='true'>&times;</button>" +
      message +
      "</div>"
  );
}

/* https://www.npmjs.com/package/qrcode */
function fCreateTransactionQR(text) {
  fTransactionOutcomeDisable();
  $('#id-tab-body-trans-qr').css('display', 'block');
  QRCode.toCanvas($('#id-tab-body-trans-qr-canvas')[0], text, function (error) {
    if (error) console.error(error);
    console.log('QR generated: ' + text);
  })
}

function fCreateAccLinkQR(text) {
  $('#id-tab-head-acc-btn-link-qr').css('display', 'block');
  QRCode.toCanvas($('#id-tab-head-acc-btn-link-qr-canvas')[0], text, function (error) {
    if (error) console.error(error);
    console.log('QR generated: ' + text);
  })
}

function fTransactionOutcomeEnable(hash) {
  $('#id-tab-body-trans-outcome').css('display', 'block');

  let html =
      "Transaction receipt: " +
      "<a href='https://ropsten.etherscan.io/tx/" + hash + "'>" + hash + "</a>";

  $('#id-tab-body-trans-outcome-p').html(html);
}

function fClearTransactionQR() {
  $('#id-tab-body-trans-qr').css('display', 'none');
}

function fClearAccLinkQR() {
  $('#id-tab-head-acc-btn-link-qr').css('display', 'none');
}

function fTransactionOutcomeDisable() {
  $('#id-tab-body-trans-outcome').css('display', 'none');
}

function fUpdateAccountList(address, balance) {
  let convert = new BigNumber(EthJS.util.stripHexPrefix(balance), 16).toString(10);
  let eth = EthJS.units.convert(convert, 'wei', 'eth');
  fTableAddRow(address, eth);
  fDropdownAddOption(address);
}

function fRx(message) {
  try {
    message = JSON.parse(message);
    let data = message.data;
    let type = data.type;
    if (type == 'console') {
      let text = data.data;
      $('#id-server-console').val($('#id-server-console').val() + text);
      $('#id-server-console').scrollTop($('#id-server-console')[0].scrollHeight);
    } if (type == "add-hw-resp") {
      fClearAccLinkQR();
      fUpdateAccountList(data.address, data.balance);
    } if (type == "transact-sign-resp") {
      fClearTransactionQR();
      fTransactionOutcomeEnable(data.transactionHash);
    } else {

    }
  } catch (err) {
    // ignore
  }
}
