package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.nullgame.NullBot.Difficulty;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LobbyUpdate(boolean open, BlockPos pos, UUID owner, int capacity, int availableMask,
    List<Member> players, List<Candidate> nearby, boolean inviteOnly, int ruleMask,
    String gameIcon, List<String> rules, String message,int firstDealer,int nextDealer) {
    public record Member(UUID id, String name, int seat, Difficulty difficulty) {
        public Member(UUID id,String name,int seat){this(id,name,seat,null);}
        public boolean bot(){return difficulty!=null;}
    }
    public record Candidate(UUID id, String name, boolean invited) {}
    public int available() { return Integer.bitCount(availableMask); }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(open); buf.writeBlockPos(pos); buf.writeUUID(owner);
        buf.writeVarInt(capacity); buf.writeVarInt(availableMask);
        buf.writeVarInt(players.size());
        for (var p : players) { buf.writeUUID(p.id); buf.writeUtf(p.name); buf.writeByte(p.seat);buf.writeByte(p.bot()?p.difficulty.ordinal()+1:0); }
        buf.writeVarInt(nearby.size());
        for (var p : nearby) { buf.writeUUID(p.id); buf.writeUtf(p.name); buf.writeBoolean(p.invited); }
        buf.writeBoolean(inviteOnly); buf.writeVarInt(ruleMask);
        buf.writeUtf(gameIcon); buf.writeVarInt(rules.size()); rules.forEach(buf::writeUtf); buf.writeUtf(message);
        buf.writeByte(firstDealer);buf.writeByte(nextDealer);
    }
    private static int count(FriendlyByteBuf buf, int limit) {
        int count = buf.readVarInt();
        if (count < 0 || count > limit) throw new IllegalArgumentException("Invalid lobby list size");
        return count;
    }
    public static LobbyUpdate read(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean(); BlockPos pos = buf.readBlockPos(); UUID owner = buf.readUUID();
        int capacity = buf.readVarInt(), mask = buf.readVarInt();
        if (capacity < 1 || capacity > 8 || (mask & ~255) != 0) throw new IllegalArgumentException("Invalid lobby limits");
        int size = count(buf,8); var players = new ArrayList<Member>(size);
        for(int i=0;i<size;i++){
            UUID id=buf.readUUID();String name=buf.readUtf(64);int seat=buf.readByte();
            int difficulty=buf.readUnsignedByte();
            if(difficulty>Difficulty.values().length)throw new IllegalArgumentException("Invalid bot difficulty");
            players.add(new Member(id,name,seat,difficulty==0?null:Difficulty.values()[difficulty-1]));
        }
        size = count(buf,64); var nearby = new ArrayList<Candidate>(size);
        for (int i=0;i<size;i++) nearby.add(new Candidate(buf.readUUID(),buf.readUtf(64),buf.readBoolean()));
        boolean inviteOnly = buf.readBoolean(); int ruleMask = buf.readVarInt();
        String icon = buf.readUtf(192); size = count(buf,16); var rules = new ArrayList<String>(size);
        for (int i=0;i<size;i++) rules.add(buf.readUtf(128));
        String message=buf.readUtf(256);int first=buf.readUnsignedByte(),next=buf.readUnsignedByte();
        if(first>1||next>2)throw new IllegalArgumentException("Invalid dealer settings");
        return new LobbyUpdate(open,pos,owner,capacity,mask,List.copyOf(players),List.copyOf(nearby),inviteOnly,
            ruleMask,icon,List.copyOf(rules),message,first,next);
    }
}
