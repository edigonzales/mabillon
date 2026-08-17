package guru.interlis.mabillon.persistence;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.springframework.stereotype.Component;

@Component
public final class CayenneUnitOfWork {

    private final CayenneRuntime runtime;

    public CayenneUnitOfWork(CayenneRuntime runtime) {
        this.runtime = runtime;
    }

    public <T> T read(Function<ObjectContext, T> work) {
        Objects.requireNonNull(work, "work");
        return work.apply(runtime.newContext());
    }

    public <T> T write(Function<ObjectContext, T> work) {
        Objects.requireNonNull(work, "work");
        ObjectContext context = runtime.newContext();
        try {
            T result = work.apply(context);
            context.commitChanges();
            return result;
        } catch (RuntimeException failure) {
            try {
                context.rollbackChanges();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    public void write(Consumer<ObjectContext> work) {
        write(context -> {
            work.accept(context);
            return null;
        });
    }
}
