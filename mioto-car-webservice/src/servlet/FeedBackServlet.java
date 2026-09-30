/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static servlet.BaseServlet._Logger;
import thrift.TFeedBack;
import thrift.TFeedBackResult;
import thrift.TListFeedBackResult;
import thrift.TUser;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class FeedBackServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer receiverId = parseIdFromRequest(req);
            if (receiverId == null || receiverId <= 0) {
                _Logger.info("Bad request receiverId id : " + receiverId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            String countFromParam = param(req, null, "count");
            String offsetFromParam = param(req, null, "offset");
            int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
            int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
            TListFeedBackResult result = ClientHolder.get().getFeedBack(receiverId, count, offset);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(get district)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get district failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("items", result.getValue());
            data.put("count", count);
            data.put("offset", offset);
            ok(resp, data);
        } catch (Exception e) {
            _Logger.error("feedback get failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> body = jsonBody(req);
            String comment = param(req, body, "comment");
            String pointStarFromParam = param(req, body, "pointStar");
            String receiverIdFromParam = param(req, body, "receiverId");
            if (comment == null || comment.trim().isEmpty()) {
                _Logger.info("Bad request comment");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bình luận không được để trống");
                return;
            }
            if (pointStarFromParam == null || pointStarFromParam.trim().isEmpty()) {
                _Logger.info("Bad request pointStar");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Điểm số không được để trống");
                return;
            }
            int pointStar = Integer.parseInt(pointStarFromParam);
            if (pointStar < 1 || pointStar > 5) {
                _Logger.info(("Bad request pointStar"));
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Điểm số chỉ được phép nằm trong khoảng từ 1 tới 5");
                return;
            }
            if (receiverIdFromParam == null || receiverIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request receiverId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id người được đánh giá không được để trống");
                return;
            }
            int receiverId = Integer.parseInt(receiverIdFromParam);
            if (receiverId <= 0) {
                _Logger.info("Bad request receiverId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id người được đánh giá không hợp lệ");
                return;
            }
            TUser sender = getUserFromRequest(req);
            if (sender == null) {
                _Logger.info("Sender is not exist");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không tồn tại");
                return;
            }
            if (receiverId == sender.getUserId()) {
                _Logger.info("senderId = receiverId is incorrect");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bạn không thể tự đánh giá chính mình");
                return;
            }
            TFeedBack feedback = new TFeedBack();
            feedback.setComment(comment);
            feedback.setCreatedAt(System.currentTimeMillis());
            feedback.setUpdatedAt(System.currentTimeMillis());
            feedback.setPointStar(pointStar);
            feedback.setReceiverId(receiverId);
            feedback.setSenderId(sender.getUserId());
            TFeedBackResult result = ClientHolder.get().createFeedBack(feedback);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(create feedback)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("Create feedback failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("feedback create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!isAdmin(req, resp)) {
                return;
            }
            Integer feedbackId = parseIdFromRequest(req);
            if (feedbackId == null || feedbackId <= 0) {
                _Logger.info("Bad request feedback id : " + feedbackId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            Map<String, Object> body = jsonBody(req);
            String comment = param(req, body, "comment");
            String pointStarFromParam = param(req, body, "pointStar");
            if (comment == null || comment.trim().isEmpty()) {
                _Logger.info("Bad request comment");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bình luận không được để trống");
                return;
            }
            if (pointStarFromParam == null || pointStarFromParam.trim().isEmpty()) {
                _Logger.info("Bad request pointStar");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Điểm số không được để trống");
                return;
            }
            int pointStar = Integer.parseInt(pointStarFromParam);
            if (pointStar < 1 || pointStar > 5) {
                _Logger.info(("Bad request pointStar"));
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Điểm số chỉ được phép nằm trong khoảng từ 1 tới 5");
                return;
            }
            TFeedBack feedback = new TFeedBack();
            feedback.setFeedbackId(feedbackId);
            feedback.setComment(comment);
            feedback.setPointStar(pointStar);
            feedback.setUpdatedAt(System.currentTimeMillis());
            TFeedBackResult result = ClientHolder.get().updateFeedBack(feedback);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(update feedback)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update feedback");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy bình luận để cập nhật");
                    return;
                }
                _Logger.error("updated feedback failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;

            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("feedback update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!isAdmin(req, resp)) {
                return;
            }
            Integer feedbackId = parseIdFromRequest(req);
            if (feedbackId == null || feedbackId <= 0) {
                _Logger.info("Bad request feedback id : " + feedbackId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TFeedBackResult result = ClientHolder.get().deleteFeedBack(feedbackId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete feedback");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete feedback");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy bình luận để xóa");
                    return;
                }
                _Logger.error("Deleted feedback failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, new TFeedBack());
        } catch (Exception e) {
            _Logger.error("delete feedback failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
