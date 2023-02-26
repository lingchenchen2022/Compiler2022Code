import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ParserErrorListener extends BaseErrorListener {
    private boolean isError;
    public ParserErrorListener(){
        isError=false;
    }

    public boolean getIsError(){
        return isError;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e){
        isError=true;
        System.err.println("Error type B at Line "+line+":"+msg);
    }

}
