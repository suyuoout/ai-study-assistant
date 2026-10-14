package com.study.assisiant;


import com.study.assisiant.mapper.StudyPlanMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/study-plan")
public class StudyPlanController {


    private final StudyPlanMapper studyPlanMapper;


    public StudyPlanController(
            StudyPlanMapper studyPlanMapper
    ){
        this.studyPlanMapper = studyPlanMapper;
    }



    /**
     * 新增学习计划
     */
    @PostMapping("/add")
    public Result<String> add(
            @RequestBody StudyPlan studyPlan,
            HttpServletRequest request
    ){


        Long userId =
                (Long) request.getAttribute("userId");


        studyPlan.setUserId(userId);


        studyPlanMapper.insert(studyPlan);


        return Result.success(
                "学习计划创建成功",
                null
        );
    }




    /**
     * 查询我的学习计划
     */
    @GetMapping("/list")
    public Result<List<StudyPlan>> list(
            HttpServletRequest request
    ){


        Long userId =
                (Long) request.getAttribute("userId");


        List<StudyPlan> list =
                studyPlanMapper.findByUserId(userId);


        return Result.success(
                "查询成功",
                list
        );
    }




    /**
     * 修改学习计划
     */
    @PutMapping("/update")
    public Result<String> update(
            @RequestBody StudyPlan studyPlan,
            HttpServletRequest request
    ){

        Long userId =
                (Long) request.getAttribute("userId");


        studyPlan.setUserId(userId);


        int result =
                studyPlanMapper.update(studyPlan);


        if(result == 0){
            return Result.error(
                    403,
                    "无权限修改该学习计划"
            );
        }


        return Result.success(
                "修改成功",
                null
        );
    }




    /**
     * 删除学习计划
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(
            @PathVariable Long id,
            HttpServletRequest request
    ){

        Long userId =
                (Long) request.getAttribute("userId");


        int result =
                studyPlanMapper.delete(id,userId);


        if(result == 0){

            return Result.error(
                    403,
                    "无权限删除该学习计划"
            );
        }


        return Result.success(
                "删除成功",
                null
        );
    }

}
