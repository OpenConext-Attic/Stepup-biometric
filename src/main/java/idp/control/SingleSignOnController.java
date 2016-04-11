package idp.control;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SingleSignOnController {

  @RequestMapping(method = RequestMethod.POST, value = "/singleSignOn")
  public ModelAndView signalSignOn(Authentication authentication) {
    System.out.println(authentication);
    //request.getRequestDispatcher(authnResponderURI).forward(request, response);
    return new ModelAndView("singleSignOn");
  }

}
