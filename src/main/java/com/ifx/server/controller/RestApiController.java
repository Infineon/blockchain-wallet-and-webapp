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
package com.ifx.server.controller;

import com.ifx.server.entity.User;
import com.ifx.server.model.*;
import com.ifx.server.service.CoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.ifx.server.EndpointConstants.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class RestApiController {

    @Autowired
    private CoreService coreService;

    /**
     * Web Frontend Access
     */

    @GetMapping(WEBAPI_PING_URL)
    @PostMapping(WEBAPI_PING_URL)
    public Response<String> processPing() {
        return coreService.restPing();
    }

    @GetMapping(WEBAPI_GET_USERNAME_URL)
    public Response<String> processGetUsername() {
        return coreService.restGetUsername();
    }

    @PostMapping(WEBAPI_SIGN_UP_URL)
    public Response<String> processRegistration(@RequestBody User userForm, BindingResult bindingResult) {
        return coreService.restUserRegistration(userForm, bindingResult);
    }

    @GetMapping(WEBAPI_SIGN_OUT_URL)
    public Response<String> processLogout(HttpServletRequest request, HttpServletResponse response) {
        return coreService.restUserSignOut(request, response);
    }

    @RequestMapping(value = WEBCONTENT_ERROR_URL, method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Object> processError(HttpServletResponse response) {
        return coreService.restError(response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_ADD_URL, method = POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AccountResp> processAccountAdd(@RequestBody Account account, HttpServletResponse response) {
        return coreService.restAccountAdd(account, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_REMOVE_URL, method = POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Object> processAccountRemove(@RequestBody Account account, HttpServletResponse response) {
        return coreService.restAccountRemove(account, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_INFO_URL, method = POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AccountResp> processAccountInfo(@RequestBody Account account, HttpServletResponse response) {
        return coreService.restAccountInfo(account, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_TRANSACT_URL, method = POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<TransactResp> processAccountTransact(@RequestBody Transact transact, HttpServletResponse response) {
        return coreService.restAccountTransact(transact, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_ADD_HW_REQ_URL, method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AddHWReqResp> processAccountAddHardwareWalletRequest(HttpServletRequest request, HttpServletResponse response) {
        return coreService.restAccountAddHardwareWalletRequest(request, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_REFRESH_URL, method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AccountRefreshResp> processAccountRefresh(HttpServletResponse response) {
        return coreService.restAccountRefresh(response);
    }


    /***************************Mobile*App*****************************/

    @RequestMapping(value = WEBAPI_ACCOUNT_TRANSACT_SIGN_URL, method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<TransactSignResp> processAccountTransactSign(TransactSign transactSign, HttpServletResponse response) {
        return coreService.restAccountTransactSign(transactSign, response);
    }

    @RequestMapping(value = WEBAPI_ACCOUNT_ADD_HW_URL, method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AddHWResp> processAccountAddHardwareWallet(AddHW addHW, HttpServletResponse response) {
        return coreService.restAccountAddHardwareWallet(addHW, response);
    }
}
