package dev.frost.frostcore.invites;


public interface InviteHandler {

    
    void onAccept(Invite invite);

    
    void onDecline(Invite invite);

    
    void onExpire(Invite invite);
}

