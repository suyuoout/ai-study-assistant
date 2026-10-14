package com.study.assisiant.service;


import com.study.assisiant.ai.AIRequest;
import com.study.assisiant.StudyPlan;
import com.study.assisiant.PlanGenerateRequest;
import java.util.List;
import java.util.Map;


public interface StudyPlanService {


    Long saveAIPlan(
            Long userId,
            AIRequest request,
            String content
    );

    List<StudyPlan> findMyPlans(Long userId);

    StudyPlan findDetail(Long id,Long userId);

    boolean deletePlan(Long id, Long userId);

    List<StudyPlan> findByUserId(Long userId);

    Map<String, Object> generatePlan(
            Long userId,
            PlanGenerateRequest request);

}
