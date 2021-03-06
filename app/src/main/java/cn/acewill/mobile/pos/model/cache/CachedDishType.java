package cn.acewill.mobile.pos.model.cache;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.structure.BaseModel;

import cn.acewill.mobile.pos.factory.AppDatabase;
import cn.acewill.mobile.pos.model.dish.DishType;


/**
 * Created by Acewill on 2016/12/1.
 */
@com.raizlabs.android.dbflow.annotation.Table(name="cached_dish_type",database =AppDatabase.class)
@ModelContainer
public class CachedDishType extends BaseModel {
    public static Gson gson = new Gson();

    public CachedDishType() {
    }

    public CachedDishType(DishType dish) {
        this.id = dish.getId();
        this.jsonObject = gson.toJson(dish);
    }

    @Column
    @PrimaryKey
    @SerializedName( "id" )
    private long id; //菜品在服务器上的id

    @Column
    @SerializedName( "json_object" )
    private String jsonObject; //菜品对象对应的json字符串

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(String jsonObject) {
        this.jsonObject = jsonObject;
    }

    public DishType toDish() {
        return gson.fromJson(this.jsonObject, DishType.class);
    }
}
