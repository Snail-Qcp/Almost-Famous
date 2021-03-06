package com.noseparte.battle.battle;


import LockstepProto.*;
import com.noseparte.battle.match.SMatch;
import com.noseparte.battle.server.Protocol;
import com.noseparte.battle.utils.JobEntity;
import com.noseparte.common.bean.Actor;
import com.noseparte.common.bean.BattleActorResult;
import com.noseparte.common.bean.BattleRoomResult;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class BattleRoom {
    Long roomId;
    Integer seed;
    List<Actor> actors;
    Long createTime;
    int state;
    int mapId;
    JobEntity quartzEntity;

    List<BattleActorResult> battleActorResults = new ArrayList<>(2);//  演员战斗结果
    BattleRoomResult battleRoomResult = new BattleRoomResult();// 房间战斗结果
    // 帧计数
    @FieldNameConstants.Exclude
    private AtomicInteger frameCount = new AtomicInteger();

    // 房间帧队列
    @FieldNameConstants.Exclude
    private LinkedBlockingQueue<byte[]> frameQueue = new LinkedBlockingQueue<>();
    // 帧存储
    private Map<Integer, Frame.Builder> storeLockStep = new ConcurrentHashMap<>();

    public void addFrame(byte[] frame) {
        try {
            synchronized (this.frameQueue) {
                frameQueue.put(frame);
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public List<byte[]> pollFrame() {
        try {
            synchronized (this.frameQueue) {
                List<byte[]> frames = new ArrayList<>(4);
                int size = frameQueue.size();
                for (int i = 0; i < size; i++) {
                    byte[] frame = frameQueue.poll();
                    frames.add(frame);
                }
                if (size == 0) {
                    frames.add(new byte[]{});
                }
                return frames;
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public void storeFrame(Frame.Builder frame) {
        storeLockStep.put(frame.getInfluenceFrameCount(), frame);
    }

    public Frame.Builder pullStoreFrame(Integer influenceFrameCount) {
        if (!storeLockStep.containsKey(influenceFrameCount)) {
            return null;
        }
        return storeLockStep.get(influenceFrameCount);
    }

    public List<Frame.Builder> pullRangeStoreFrame(int src, int dest) {
        List<Frame.Builder> frames = new ArrayList<>();
        if (src == dest) {
            frames.add(pullStoreFrame(src));
            return frames;
        }
        for (int i = dest; 0 < i && src <= i; i--) {
            frames.add(pullStoreFrame(i));
        }
        return frames;
    }

    public int nextFrameNumber() {
        return frameCount.getAndIncrement();
    }

    /**
     * 拼装房间信息协议
     */
    public Protocol toS2CMatch() {
        S2CMatch.Builder s2CMatch = S2CMatch.newBuilder();
        for (Actor actor : this.actors) {
            BattleActor.Builder battleActor = BattleActor.newBuilder().setRoleId(actor.getRoleId()).addAllCardIds(actor.getUserCards())
                    .setAgi(actor.getAgi()).setIq(actor.getIq()).setStr(actor.getStr()).setRoleName(actor.getRoleName()).setSchool(actor.getSchoolId())
                    .setRankId(actor.getBattleRankBean().getRankId()).setStarCount(actor.getBattleRankBean().getStarCount());
            s2CMatch.addActors(battleActor.build());
        }
        //
        byte[] respMsg = s2CMatch.setSeed(this.seed).setMapId(this.mapId).setBattleStartTime(this.createTime).build().toByteArray();
        Protocol p = new SMatch();
        p.setType(NetMessage.S2C_Match_VALUE);
        p.setMsg(respMsg);

        return p;
    }

    public void collectBattleResult(BattleActorResult battleActorResult) {
        battleActorResults.add(battleActorResult);
    }


    public Protocol toS2CBattleEnd() {
        S2CBattleEnd.Builder resp = S2CBattleEnd.newBuilder()
                .addAllWinners(battleRoomResult.getWinners())
                .addAllLosers(battleRoomResult.getLosers());

        Protocol p = new SBattleEnd();
        p.setType(NetMessage.S2C_BattleEnd_VALUE);
        p.setMsg(resp.build().toByteArray());

        return p;
    }

}
