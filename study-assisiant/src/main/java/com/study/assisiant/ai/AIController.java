package com.study.assisiant.ai;


import com.study.assisiant.Result;
import com.study.assisiant.service.StudyPlanService;
import com.study.assisiant.StudyPlan;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/ai")
public class AIController {


    @Autowired
    private AIService aiService;


    @Autowired
    private StudyPlanService studyPlanService;



    @PostMapping("/generate-plan")
    public Result generatePlan(
            @RequestBody AIRequest request,
            HttpServletRequest httpRequest
    ){


        // 获取当前用户id

        Long userId =
                (Long) httpRequest
                        .getAttribute("userId");



        // 调用AI
        String content;
        try {
            content =
                    aiService.generatePlan(request);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return Result.error(
                    500,
                    "AI生成失败，请稍后重试");
        }



        // 保存数据库

        Long id =
                studyPlanService.saveAIPlan(
                        userId,
                        request,
                        content
                );



        return Result.success(id);

    }

    @GetMapping("/history")
    public Result history(HttpServletRequest request){

        Long userId =
                (Long) request.getAttribute("userId");


        List<StudyPlan> list =
                studyPlanService.findMyPlans(userId);


        return Result.success(list);
    }

    @GetMapping("/detail/{id}")
    public Result detail(
            @PathVariable Long id,
            HttpServletRequest request
    ){

        Long userId =
                (Long)request.getAttribute("userId");


        StudyPlan plan =
                studyPlanService.findDetail(id,userId);


        if(plan==null){
            return Result.error(403,"无权限查看");
        }


        return Result.success(plan);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(
            @PathVariable Long id,
            HttpServletRequest request
    ){


        Long userId =
                (Long)request.getAttribute("userId");


        boolean success =
                studyPlanService.deletePlan(id,userId);



        if(!success){

            return Result.error(
                    403,
                    "无权限删除该学习计划"
            );

        }


        return Result.success(null);

    }

    @GetMapping("/list")
    public Result list(
            HttpServletRequest request
    ){

        Long userId =
                (Long) request.getAttribute("userId");


        List<StudyPlan> list =
                studyPlanService.findByUserId(userId);


        return Result.success(list);
    }


}
