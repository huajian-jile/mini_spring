package liu.spring.webmvc;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装模型与视图名（与 Spring 的 ModelAndView 一致）。
 * 用于非 REST 场景；REST 直接写 JSON 时可为 null 或仅用 model 传数据。
 */
public class ModelAndView {

    private String viewName;
    private final Map<String, Object> model = new HashMap<>();

    public ModelAndView() {}
    public ModelAndView(String viewName) { this.viewName = viewName; }

    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }
    public Map<String, Object> getModel() { return model; }
    public ModelAndView addObject(String name, Object value) { model.put(name, value); return this; }
}
