package idp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.webflow.execution.repository.FlowExecutionRestorationFailureException;

@ControllerAdvice
public class TestControllerAdvice {

	@ResponseBody
    @ExceptionHandler
    public ResponseEntity<String> handleFlowExecutionRestorationFailureExceptionException(FlowExecutionRestorationFailureException ex) {

    	return new ResponseEntity<String>("<html><body>Back button error!</body></html>", HttpStatus.BAD_REQUEST);
    }
}