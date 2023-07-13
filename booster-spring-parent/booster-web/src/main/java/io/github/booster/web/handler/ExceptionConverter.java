package io.github.booster.web.handler;

import io.github.booster.web.handler.response.WebException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ExceptionConverter {

    public static class GenericExceptionHandler implements ExceptionHandler<Throwable> {

        @Override
        public WebException handle(@NonNull Throwable exception) {
            return this.createResponse(
                    exception,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.name()
            );
        }

        @Override
        public Class<Throwable> handles() {
            return Throwable.class;
        }
    }

    private final List<ExceptionHandler<? extends Throwable>> exceptionHandlers;

    private final GenericExceptionHandler genericExceptionHandler = new GenericExceptionHandler();

    public ExceptionConverter(
            List<ExceptionHandler<? extends Throwable>> exceptionHandlers
    ) {
        this.exceptionHandlers = CollectionUtils.isEmpty(exceptionHandlers) ?
                List.of() :
                exceptionHandlers;
    }

    public WebException handle(Throwable throwable) {
        WebException result = null;

        for (ExceptionHandler<?> handler: this.exceptionHandlers) {
            if (handler.canHandle(throwable)) {
                result = handler.convert(throwable);
                break;
            }
        }
        if (result == null) {
            return this.genericExceptionHandler.convert(throwable);
        }
        return result;
    }
}
