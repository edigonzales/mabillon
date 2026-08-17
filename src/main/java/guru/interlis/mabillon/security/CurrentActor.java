package guru.interlis.mabillon.security;

import java.util.Set;

public interface CurrentActor {

    ActorId id();

    String username();

    String displayName();

    Set<MabillonRole> roles();
}
