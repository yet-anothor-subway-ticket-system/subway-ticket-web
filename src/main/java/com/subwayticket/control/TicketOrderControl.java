package com.subwayticket.control;

import com.subwayticket.database.control.SubwayInfoDBHelperBean;
import com.subwayticket.database.control.SystemDBHelperBean;
import com.subwayticket.database.control.TicketOrderDBHelperBean;
import com.subwayticket.database.model.Account;
import com.subwayticket.database.model.SubwayStation;
import com.subwayticket.database.model.TicketOrder;
import com.subwayticket.database.model.TicketPrice;
import com.subwayticket.model.PublicResultCode;
import com.subwayticket.model.request.ExtractTicketRequest;
import com.subwayticket.model.request.PayOrderRequest;
import com.subwayticket.model.request.RefundOrderRequest;
import com.subwayticket.model.request.SubmitOrderRequest;
import com.subwayticket.model.result.PayOrderResult;
import com.subwayticket.model.result.RefundOrderResult;
import com.subwayticket.model.result.Result;
import com.subwayticket.model.result.SubmitOrderResult;
import com.subwayticket.util.BundleUtil;
import com.subwayticket.util.SecurityUtil;

import javax.ejb.EJBException;
import javax.servlet.ServletRequest;
import java.util.Date;

/**
 * Created by zhou-shengyun on 7/4/16.
 */
public class TicketOrderControl {
    private static Object orderGlobalLock = new Object();
    private static long timestamp = 0;
    private static int serialNumber = 0;
    public static final int MAX_TICKET_AMOUNT = 10;

    public static Result submitOrder(ServletRequest req, SubwayInfoDBHelperBean subwayInfoDBHelperBean, Account user, SubmitOrderRequest submitOrderRequest){
        if(submitOrderRequest.getAmount() <= 0)
            return new Result(PublicResultCode.ORDER_SUBMIT_AMOUNT_ILLEGAL, BundleUtil.getString(req, "TipOrderAmountIllegal"));
        else if(submitOrderRequest.getAmount() > MAX_TICKET_AMOUNT)
            return new Result(PublicResultCode.ORDER_SUBMIT_AMOUNT_EXCESS, BundleUtil.getString(req, "TipOrderAmountExcess"));
        TicketPrice ticketPrice = subwayInfoDBHelperBean.getTicketPrice(submitOrderRequest.getStartStationID(), submitOrderRequest.getEndStationID());
        if(ticketPrice == null)
            return new Result(PublicResultCode.ORDER_SUBMIT_ROUTE_NOT_EXIST, BundleUtil.getString(req, "TipSubwayRouteNotExist"));
        try{
            String orderID;
            Date date;
            synchronized (orderGlobalLock){
                date = new Date();
                if(date.getTime() > timestamp) {
                    timestamp = date.getTime();
                    serialNumber = 0;
                }
                orderID = "ST" + date.getTime() + String.format("%05d", serialNumber);
                serialNumber++;
            }
            TicketOrder newOrder = new TicketOrder(orderID, date, user, new SubwayStation(submitOrderRequest.getStartStationID()),
                                                   new SubwayStation(submitOrderRequest.getEndStationID()), ticketPrice.getPrice(), submitOrderRequest.getAmount());
            subwayInfoDBHelperBean.create(newOrder);
            subwayInfoDBHelperBean.refresh(newOrder);
            return new SubmitOrderResult(PublicResultCode.SUCCESS, BundleUtil.getString(req, "TipOrderSubmitSuccess"), newOrder);
        }catch (EJBException e){
            e.printStackTrace();
        }
        return new Result(PublicResultCode.ORDER_SUBMIT_ERROR, BundleUtil.getString(req, "TipOrderSubmitError"));
    }


    public static Result cancelOrder(ServletRequest req, SystemDBHelperBean dbHelperBean, Account user, String orderId){
        TicketOrder ticketOrder = (TicketOrder) dbHelperBean.find(TicketOrder.class, orderId);
        if(ticketOrder == null)
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(!ticketOrder.getUser().getPhoneNumber().equals(user.getPhoneNumber()))
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(ticketOrder.getStatus() != TicketOrder.ORDER_STATUS_NOT_PAY)
            return new Result(PublicResultCode.ORDER_CANCEL_NOT_CANCELABLE, BundleUtil.getString(req, "TipOrderNotCancelable"));
        dbHelperBean.remove(ticketOrder);
        return new Result(PublicResultCode.SUCCESS, BundleUtil.getString(req, "TipOrderCancelSuccess"));
    }

    public static Result payOrder(ServletRequest req, SystemDBHelperBean dbHelperBean, Account user, PayOrderRequest payOrderRequest){
        TicketOrder ticketOrder = (TicketOrder) dbHelperBean.find(TicketOrder.class, payOrderRequest.getOrderId());
        if(ticketOrder == null)
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(!ticketOrder.getUser().getPhoneNumber().equals(user.getPhoneNumber()))
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(ticketOrder.getStatus() != TicketOrder.ORDER_STATUS_NOT_PAY)
            return new Result(PublicResultCode.ORDER_PAY_NOT_PAYABLE, BundleUtil.getString(req, "TipOrderNotPayable"));
        ticketOrder.setDrawAmount(0);
        ticketOrder.setStatus(TicketOrder.ORDER_STATUS_NOT_DRAW_TICKET);
        ticketOrder.setTicketKey(SecurityUtil.getExtractCode(user.getPhoneNumber()));
        dbHelperBean.merge(ticketOrder);
        return new PayOrderResult(PublicResultCode.SUCCESS, BundleUtil.getString(req, "TipOrderPaySuccess"), ticketOrder.getTicketKey());
    }

    public static Result refundOrder(ServletRequest req, SystemDBHelperBean dbHelperBean, Account user, RefundOrderRequest refundOrderRequest){
        TicketOrder ticketOrder = (TicketOrder) dbHelperBean.find(TicketOrder.class, refundOrderRequest.getOrderId());
        if(ticketOrder == null)
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(!ticketOrder.getUser().getPhoneNumber().equals(user.getPhoneNumber()))
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(ticketOrder.getStatus() != TicketOrder.ORDER_STATUS_NOT_DRAW_TICKET || ticketOrder.getDrawAmount() >= ticketOrder.getAmount())
            return new Result(PublicResultCode.ORDER_REFUND_NOT_REFUNDABLE, BundleUtil.getString(req, "TipOrderNotRefundable"));
        ticketOrder.setStatus(TicketOrder.ORDER_STATUS_REFUNDED);
        ticketOrder.setTicketKey(null);
        dbHelperBean.merge(ticketOrder);
        return new RefundOrderResult(PublicResultCode.SUCCESS, BundleUtil.getString(req, "TipOrderRefundSuccess"),
                                     ticketOrder.getTicketPrice() * (ticketOrder.getAmount() - ticketOrder.getDrawAmount()));
    }

    public static Result extractTicket(ServletRequest req, TicketOrderDBHelperBean dbHelperBean, ExtractTicketRequest extractTicketRequest){
        if(extractTicketRequest.getExtractAmount() <= 0)
            return new Result(PublicResultCode.TICKET_EXTRACT_AMOUNT_ILLEGAL, BundleUtil.getString(req, "TipExtractTicketAmountIllegal"));
        TicketOrder ticketOrder = dbHelperBean.getOrderByExtractCode(extractTicketRequest.getExtractCode());
        if(ticketOrder == null)
            return new Result(PublicResultCode.ORDER_NOT_EXIST, BundleUtil.getString(req, "TipOrderNotExist"));
        if(ticketOrder.getStatus() != TicketOrder.ORDER_STATUS_NOT_DRAW_TICKET || ticketOrder.getAmount() == ticketOrder.getDrawAmount())
            return new Result(PublicResultCode.TICKET_EXTRACT_NOT_EXTRACTABLE, BundleUtil.getString(req, "TipExtractTicketNotExtractable"));
        if(extractTicketRequest.getExtractAmount() > ticketOrder.getAmount() - ticketOrder.getDrawAmount())
            return new Result(PublicResultCode.TICKET_EXTRACT_AMOUNT_EXCESS, BundleUtil.getString(req, "TipExtractTicketAmountExcess"));

        ticketOrder.setDrawAmount(ticketOrder.getDrawAmount() + extractTicketRequest.getExtractAmount());
        if(ticketOrder.getAmount() == ticketOrder.getDrawAmount()) {
            ticketOrder.setStatus(TicketOrder.ORDER_STATUS_FINISHED);
            ticketOrder.setTicketKey(null);
        }
        dbHelperBean.merge(ticketOrder);
        return new Result(PublicResultCode.SUCCESS, BundleUtil.getString(req, "TipExtractTicketSuccess"));
    }
}
