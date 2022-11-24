package main.services;

import lombok.Data;
import main.dto.indexing.IndexingResponse;
@Data
public class IndexingResponseService {

    public IndexingResponse getTrueResponse() {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    public IndexingResponse getFalseResponse(String error){
        IndexingResponse response = new IndexingResponse();
        response.setResult(false);
        response.setError(error);
        return response;
    }
}
