package Environment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExceptionStatistic {

    private final Map<ExceptionType, Integer> statistic = new HashMap<>();
    
    private enum ExceptionType {
        UnknownException,
        PlayerException,
        LoadException,
        ItemsException,
        AssetsException,
        EnvironmentException,
        ProgrammerError
     }

    private void recordException(ExceptionType type) {
        getStatistic().compute(type, (t, count) -> (count == null ? 0 : count) + 1);
    }

    public void recordUnknownException() { 
        recordException(ExceptionType.UnknownException);
    }

    public void recordPlayerException() { 
        recordException(ExceptionType.PlayerException);
    }

    public void recordLoadException() { 
        recordException(ExceptionType.LoadException);
    }

    public void recordItemsException() { 
        recordException(ExceptionType.ItemsException);
    }

    public void recordAssetsException() { 
        recordException(ExceptionType.AssetsException);
    }

    public void recordEnvironmentException() { 
        recordException(ExceptionType.EnvironmentException);
    }

    public void recordProgrammerError() { 
        recordException(ExceptionType.ProgrammerError);
    }


    public Map<ExceptionType, Integer> getStatistic() {
        return Collections.unmodifiableMap(statistic);
    }

}
