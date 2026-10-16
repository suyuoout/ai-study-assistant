package com.study.assisiant.plan;

import com.study.assisiant.ai.AiCallContext;

import java.util.List;
import java.util.Map;

public interface StudyPlanService {

    /** AI 生成计划并自动拆解为每日任务 */
    Map<String, Object> generatePlan(PlanGenerateRequest request,
                                     AiCallContext ctx);

    List<StudyPlan> findByUserId(Long userId);

    /** 取计划详情；不属于该用户时抛 403 */
    StudyPlan requireOwned(Long userId, Long planId);

    StudyPlan updatePlan(Long userId, Long planId, PlanUpdateRequest request);

    void deletePlan(Long userId, Long planId);
}
