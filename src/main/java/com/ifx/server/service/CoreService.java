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
package com.ifx.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifx.server.entity.User;
import com.ifx.server.model.*;
import com.ifx.server.repository.*;
import com.ifx.server.service.security.UserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.IntStream;

@Service
public class CoreService {

    Logger logger = LoggerFactory.getLogger(CoreService.class);

    private Random random = new Random();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRepositoryService userRepoService;
    @Autowired
    private UserValidator userValidator;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;
    @Autowired
    private EthereumService eth;

    public CoreService() {
    }

    private String viewAddModelAttributeUsername(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken == false) {
            model.addAttribute("username", " " + authentication.getName() + " | Log me out");
            return authentication.getName();
        }
        return null;
    }

    public String viewHome(Model model) {
        viewAddModelAttributeUsername(model);
        return "home";
    }

    public String viewEntry(Model model) {
        viewAddModelAttributeUsername(model);
        model.addAttribute("userCount", userRepository.count());
        return "entry";
    }

    public String viewDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        model.addAttribute("user", user.getUsername());
        model.addAttribute("accounts", user.getAccounts());

        viewAddModelAttributeUsername(model);
        return "dashboard";
    }

    public Response<String> restPing() {
        getSecurityFilterChain();
        return new Response<String>(Response.STATUS_OK, "Hello Client");
    }

    public Response<String> restGetUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new Response<String>(Response.STATUS_OK, authentication.getName());
    }

    public Response<String> restUserRegistration(User userForm, BindingResult bindingResult) {
        userValidator.validate(userForm, bindingResult);

        if (bindingResult.hasErrors()) {
            if (bindingResult.toString().contains("exceeded.max.reg.user"))
                return new Response<String>(Response.STATUS_ERROR, "exceeded maximum user registration, please contact server admin");
            return new Response<String>(Response.STATUS_ERROR, null);
        }

        userRepoService.save(userForm);

        return new Response<String>(Response.STATUS_OK, null);
    }

    public Response<String> restUserSignOut(HttpServletRequest request, HttpServletResponse response) {
        try {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            for(Cookie cookie : request.getCookies()) {
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
            return new Response<String>(Response.STATUS_OK, null);
        } catch (Exception e) {
            return new Response<String>(Response.STATUS_ERROR, null);
        }
    }

    public Response<Object> restError(HttpServletResponse response) {
        return new Response<Object>(Response.STATUS_OK, Integer.toString(response.getStatus()), null);
    }

    public Response<AccountResp> restAccountAdd(Account account, HttpServletResponse response) {
        try {
            account.setAddress(eth.toChecksumAddress(account.getAddress()));
            if (!eth.isValidAddress(account.getAddress()))
                throw new Exception("Malformed address.");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            Account createdAcc = addAccount(user, account.getAddress());

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null,
                    new AccountResp(createdAcc.getAddress(), createdAcc.getBalance()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }
    }

    public Response<Object> restAccountRemove(Account account, HttpServletResponse response) {
        try {
            account.setAddress(eth.toChecksumAddress(account.getAddress()));
            if (!eth.isValidAddress(account.getAddress()))
                throw new Exception("Malformed address.");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            int found = -1;

            if (user.getAccounts() != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<Account> list = mapper.convertValue(user.getAccounts(), new TypeReference<List<Account>>() {});
                found = IntStream.range(0, user.getAccounts().size())
                        .filter(i -> list.get(i).getAddress().equals(account.getAddress()))
                        .findFirst()
                        .getAsInt();
            }

            user.getAccounts().remove(found);
            userRepository.save(user);
            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }

    }

    public Response<AccountResp> restAccountInfo(Account account, HttpServletResponse response) {
        try {
            account.setAddress(eth.toChecksumAddress(account.getAddress()));
            if (!eth.isValidAddress(account.getAddress()))
                throw new Exception("Malformed address.");

            String balance = eth.getBalance(account.getAddress());
            String transactionCount = eth.getTransactionCount(account.getAddress());
            String gasPrice = eth.getGasPrice();
            String estimatedGas = eth.estimateEtherTransactionGas();
            AccountResp resp = new AccountResp(account.getAddress(),
                    balance, transactionCount, gasPrice, estimatedGas);

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, resp);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }
    }

    public Response<TransactResp> restAccountTransact(Transact transact, HttpServletResponse response) {
        try {
            transact.setFrom(eth.toChecksumAddress(transact.getFrom()));
            if (!eth.isValidAddress(transact.getFrom()) ||
                !eth.isValidAddress(transact.getTo()))
                throw new Exception("Malformed address.");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            if (!eth.verifyTransactionHash(transact))
                throw new Exception("Invalid hash.");

            /*if (!eth.verifySendTransactionWithSoftWallet(transact))
                throw new Exception("Unable to process transaction.");*/

            TransactResp transactResp = null;
            if (user.getAccounts() != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<Account> list = mapper.convertValue(user.getAccounts(), new TypeReference<List<Account>>() {});

                OptionalInt index = IntStream.range(0, list.size())
                        .filter(i -> list.get(i).getAddress().equals(transact.getFrom()))
                        .findFirst();

                if (!index.isPresent()) {
                    throw new Exception("Account not found.");
                }

                int found = index.getAsInt();
                Account acc = list.get(found);

                user.setTransactionToken(eth.sha3(transact.getSerialized()).substring(2));
                acc.setSerializedTransaction(transact.getSerialized());
                user.getAccounts().remove(found);
                user.getAccounts().add(acc);
                userRepository.save(user);
                transactResp = new TransactResp(transact.getFrom(), user.getTransactionToken());
            }

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, transactResp);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }

    }

    public Response<AddHWReqResp> restAccountAddHardwareWalletRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            byte[] tBytes = new byte[32];
            random.nextBytes(tBytes);
            String token = eth.sha3(Numeric.toHexString(tBytes)).substring(2);

            AddHWReqResp resp = new AddHWReqResp(token);

            user.setLinkToken(token);
            userRepository.save(user);

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, resp);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }

    }

    public Response<TransactSignResp> restAccountTransactSign(TransactSign transactSign, HttpServletResponse response) {
        try {
            if (transactSign.getToken() == null || transactSign.getSignature() == null ||
                transactSign.getPubkey() == null )
                throw new Exception("Token, public key, or signature field not defined.");

            User user = userRepository.findByTransactionToken(transactSign.getToken());
            if (user == null)
                throw new Exception("Invalid token.");

            Account account = null;
            int found = 0;
            String deriveAddr = eth.toAddress(transactSign.getPubkey());

            if (user.getAccounts() != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<Account> list = mapper.convertValue(user.getAccounts(), new TypeReference<List<Account>>() {});
                OptionalInt index = IntStream.range(0, list.size())
                        .filter(i -> list.get(i).getAddress().equals(deriveAddr))
                        .findFirst();
                if (!index.isPresent())
                    throw new Exception("No pending transaction.");

                found = index.getAsInt();
                account = list.get(found);
            } else {
                throw new Exception("No pending transaction.");
            }

            String transactionHash = eth.sendTransaction(account.getSerializedTransaction(),
                    transactSign.getPubkey(), transactSign.getSignature());
            if (transactionHash == null)
                throw new Exception("Unable to process transaction.");

            user.setTransactionToken(null);
            user.getAccounts().remove(found);
            account.setSerializedTransaction(null);
            user.getAccounts().add(account);
            userRepository.save(user);

            // push notification
            simpMessagingTemplate.convertAndSendToUser(user.getUsername(), "/topic/private",
                    new Response<TransactSignResp>(Response.STATUS_OK, new TransactSignResp(transactionHash)));

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }

    }

    public Response<AddHWResp> restAccountAddHardwareWallet(AddHW addHW, HttpServletResponse response) {
        try {
            if (addHW.getToken() == null || addHW.getPubkey() == null)
                throw new Exception("Token or public key field not defined.");

            User user = userRepository.findByLinkToken(addHW.getToken());
            if (user == null)
                throw new Exception("Invalid token.");

            String address = eth.toAddress(addHW.getPubkey());
            Account createdAcc = addAccount(user, address);

            user.setLinkToken(null);
            userRepository.save(user);

            // push notification
            simpMessagingTemplate.convertAndSendToUser(user.getUsername(), "/topic/private",
                    new Response<AddHWResp>(Response.STATUS_OK, new AddHWResp(createdAcc.getAddress(),
                            createdAcc.getBalance())));

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }
    }

    public Response<AccountRefreshResp> restAccountRefresh(HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            List<Account> accounts = user.getAccounts();

            if (accounts == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                return new Response<>(Response.STATUS_OK, null, null);
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Account> list = mapper.convertValue(user.getAccounts(), new TypeReference<List<Account>>() {});
            list.forEach(account -> {
                String address = account.getAddress();
                String balance = eth.getBalance(address);
                account.setBalance(balance);
            });

            user.setAccounts(list);
            userRepository.save(user);

            AccountRefreshResp resp = new AccountRefreshResp(list);
            response.setStatus(HttpServletResponse.SC_OK);
            return new Response<>(Response.STATUS_OK, null, resp);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response<>(Response.STATUS_ERROR, e.toString(), null);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    private Account addAccount(User user, String address) throws Exception {
        Account account = new Account(address, null, null);

        String balance = eth.getBalance(address);
        Account existingAcc = null;

        if (user.getAccounts() != null) {
            ObjectMapper mapper = new ObjectMapper();
            List<Account> list = mapper.convertValue(user.getAccounts(), new TypeReference<List<Account>>() {});
            existingAcc = list.stream()
                    .filter(c -> c.getAddress().equals(account.getAddress()))
                    .findAny().orElse(null);
        }

        account.setBalance(balance);
        if (existingAcc != null) {
            existingAcc.setBalance(balance);
        } else  {
            if (user.getAccounts() == null) {
                List<Account> accounts = new ArrayList<>();
                accounts.add(account);
                user.setAccounts(accounts);
            } else {
                user.getAccounts().add(account);
            }
        }

        userRepository.save(user);

        return account;
    }

    private void getSecurityFilterChain() {
        FilterChainProxy filterChainProxy = (FilterChainProxy) springSecurityFilterChain;
        List<SecurityFilterChain> list = filterChainProxy.getFilterChains();
        list.stream()
                .flatMap(chain -> chain.getFilters().stream())
                .forEach(filter -> System.out.println(filter.getClass()));
    }

    private void consoleLog(String username, String m) {
        try {
            if (username != null && username != "")
                simpMessagingTemplate.convertAndSendToUser(username, "/topic/private",
                        new Response<Console>(Response.STATUS_OK, new Console(m)));
        } catch (Exception e) {

        }
    }
}
