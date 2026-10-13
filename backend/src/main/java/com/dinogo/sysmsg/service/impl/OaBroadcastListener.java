package com.dinogo.sysmsg.service.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.service.OaBroadcastRequested;
import com.dinogo.sysmsg.service.RecordService;

/** 模擬專案版本：Send commit 後，以固定批次非同步建立 OA Record。 */
@Component
public class OaBroadcastListener {
    private static final Logger LOG=Logger.getLogger(OaBroadcastListener.class.getName());
    private final MemberSysmsgProviderService members; //Client-sysmsg
    private final SellerSysmsgProviderService sellers; //Client-sysmsg
    private final RecordService records; private final int batchSize;
    public OaBroadcastListener(MemberSysmsgProviderService members, //Client-sysmsg
            SellerSysmsgProviderService sellers,RecordService records, //Client-sysmsg
            @Value("${sysmsg.oa-batch-size:100}") int batchSize){
        this.members=members;this.sellers=sellers;this.records=records;this.batchSize=Math.max(1,batchSize);
    }
    @Async
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void broadcast(OaBroadcastRequested event){
        List<Integer> memberIds=members.getAllMembers().stream().map(m->m.memberId()).distinct().toList(); //Client-sysmsg
        List<Integer> sellerIds=sellers.getAllSellers().stream().map(s->s.sellerId()).distinct().toList(); //Client-sysmsg
        createBatches(event.sendId(),memberIds,false); createBatches(event.sendId(),sellerIds,true);
    }
    private void createBatches(Integer sendId,List<Integer> ids,boolean seller){
        for(int from=0;from<ids.size();from+=batchSize){
            List<Integer> batch=ids.subList(from,Math.min(from+batchSize,ids.size()));
            try{records.createRecords(sendId,seller?List.of():batch,seller?batch:List.of());}
            catch(RuntimeException ex){LOG.log(Level.WARNING,"OA 批次建立失敗，sendId="+sendId+", from="+from,ex);}
        }
    }
}
