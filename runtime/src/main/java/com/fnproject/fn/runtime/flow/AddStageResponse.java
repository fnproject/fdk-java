package com.fnproject.fn.runtime.flow;

import com.fasterxml.jackson.annotation.JsonProperty;

class AddStageResponse {
    @JsonProperty("flow_id")
    String flowId;
    @JsonProperty("stage_id")
    String stageId;
}
