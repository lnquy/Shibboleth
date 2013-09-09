package idp;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TestController {

    @Autowired
    private ApplicationContext appContext;

    @RequestMapping("/contextInfo")
    public String contextInfo(Model model) {
    	ArrayList<ApplicationContext> contexts = new ArrayList<>();
    	
    	ApplicationContext current = appContext;
    	while (current != null) {
    		contexts.add(current);
    		current = current.getParent();
    	}
    	
        model.addAttribute("appContextList", contexts);
        return "contextInfo";
    }
    
}
