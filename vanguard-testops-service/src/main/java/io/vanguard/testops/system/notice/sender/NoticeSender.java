package io.vanguard.testops.system.notice.sender;


import io.vanguard.testops.system.notice.MessageDetail;
import io.vanguard.testops.system.notice.NoticeModel;

public interface NoticeSender {
    void send(MessageDetail messageDetail, NoticeModel noticeModel);
}
