

@Data
@Schema(description = "标签新增、编辑入参")
public class TagSaveDTO {

    @Schema(description = "标签名", example = "Java", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "must not be blank")
    @Size(min = 2, max = 20, message = "长度必须在 2-20 字符之间")
    private String name;

    @Schema(description = "标签颜色(HEX)")
    @Size(max = 20, message = "颜色值最多20个字符")
    private String color;
}