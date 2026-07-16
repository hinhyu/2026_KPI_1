package com.sample.order;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legacy")
public class LegacyOrderController {

    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    public String listOrders(@RequestParam(name = "status") String status) {
        return status;
    }

    @RequestMapping(path = {"/orders/{orderId}", "/order/{orderId}"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public String getLegacyOrder(@PathVariable("orderId") String orderId) {
        return orderId;
    }

    @RequestMapping(value = "/orders", method = RequestMethod.DELETE)
    public void deleteOrders(@RequestParam String reason) {
    }
}
