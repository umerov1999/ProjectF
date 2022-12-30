package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.ICommentsApi
import dev.ragnarok.fenrir.api.model.response.CustomCommentsResponse
import dev.ragnarok.fenrir.api.services.ICommentsService
import io.reactivex.rxjava3.core.Single

internal class CommentsApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), ICommentsApi {
    override fun get(
        sourceType: String?,
        ownerId: Int,
        sourceId: Int,
        offset: Int?,
        count: Int?,
        sort: String?,
        startCommentId: Int?,
        threadComment: Int?,
        accessKey: String?,
        fields: String?
    ): Single<CustomCommentsResponse> {
        val thread_id = threadComment ?: 0
        return provideService(ICommentsService(), TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service["var comment_id = Args.comment_id;\n" +
                        "var owner_id = Args.owner_id;\n" +
                        "var source_id = Args.source_id;\n" +
                        "var start_comment_id = Args.start_comment_id;\n" +
                        "var offset = Args.offset;\n" +
                        "var count = Args.count;\n" +
                        "var sort = Args.sort;\n" +
                        "var access_key = Args.access_key;\n" +
                        "var source_type = Args.source_type;\n" +
                        "var fields = Args.fields;\n" +
                        "\n" +
                        "var positive_group_id;\n" +
                        "if(owner_id < 0){\n" +
                        "    positive_group_id = -owner_id;\n" +
                        "} else {\n" +
                        "    positive_group_id = owner_id;\n" +
                        "}\n" +
                        "\n" +
                        "var admin_level = 0;\n" +
                        "if(owner_id < 0){\n" +
                        "    admin_level = API.groups.getById({\"v\":\"" + Constants.API_VERSION + "\",\"group_id\":positive_group_id,\n" +
                        "        \"fields\":\"admin_level\"})[0].admin_level;\n" +
                        "}\n" +
                        "\n" +
                        "var result;\n" +
                        "if(source_type == \"post\"){\n" +
                        "    if(comment_id != 0)\n" +
                        "    {\n" +
                        "    \tresult = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \n" +
                        "        \"need_likes\":\"1\", \n" +
                        "        \"start_comment_id\":start_comment_id, \n" +
                        "        \"offset\":offset, \n" +
                        "        \"count\":count, \n" +
                        "        \"sort\":sort, \n" +
                        "        \"preview_length\":\"0\", \n" +
                        "        \"extended\":\"1\",\n" +
                        "        \"comment_id\":comment_id,\n" +
                        "        \"thread_items_count\":\"10\",\n" +
                        "        \"fields\":fields\n" +
                        "    });\n" +
                        "    }\n" +
                        "    else\n" +
                        "    {\n" +
                        "    result = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \n" +
                        "        \"need_likes\":\"1\", \n" +
                        "        \"start_comment_id\":start_comment_id, \n" +
                        "        \"offset\":offset, \n" +
                        "        \"count\":count, \n" +
                        "        \"sort\":sort, \n" +
                        "        \"preview_length\":\"0\", \n" +
                        "        \"extended\":\"1\",\n" +
                        "        \"thread_items_count\":\"10\",\n" +
                        "        \"fields\":fields\n" +
                        "    });\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"photo\"){\n" +
                        "    result = API.photos.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"photo_id\":source_id, \n" +
                        "        \"need_likes\":\"1\", \n" +
                        "        \"start_comment_id\":start_comment_id, \n" +
                        "        \"offset\":offset, \n" +
                        "        \"count\":count, \n" +
                        "        \"sort\":sort, \n" +
                        "        \"extended\":\"1\",\n" +
                        "        \"fields\":fields,\n" +
                        "        \"access_key\":access_key});\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"video\"){\n" +
                        "    result = API.video.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"video_id\":source_id, \n" +
                        "        \"need_likes\":\"1\", \n" +
                        "        \"start_comment_id\":start_comment_id, \n" +
                        "        \"offset\":offset, \n" +
                        "        \"count\":count, \n" +
                        "        \"sort\":sort, \n" +
                        "        \"extended\":\"1\",\n" +
                        "        \"fields\":fields\n" +
                        "    });\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"topic\"){\n" +
                        "    result = API.board.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"group_id\":positive_group_id, \n" +
                        "        \"topic_id\":source_id, \n" +
                        "        \"need_likes\":\"1\", \n" +
                        "        \"start_comment_id\":start_comment_id, \n" +
                        "        \"offset\":offset, \n" +
                        "        \"count\":count, \n" +
                        "        \"sort\":sort, \n" +
                        "        \"extended\":\"1\"});\n" +
                        "}\n" +
                        "\n" +
                        "var first_id;\n" +
                        "if(source_type == \"post\"){\n" +
                        "   if(comment_id != 0)\n" +
                        "   {\n" +
                        "   first_id = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"asc\", \"comment_id\":comment_id, \"thread_items_count\":\"10\"}).items[0].id;\n" +
                        "   }\n" +
                        "   else\n" +
                        "   {\n" +
                        "   first_id = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"asc\", \"thread_items_count\":\"10\"}).items[0].id;\n" +
                        "   }\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"photo\"){\n" +
                        "   first_id = API.photos.getComments({\"v\":\"" + Constants.API_VERSION + "\",\n" +
                        "        \"owner_id\":owner_id, \n" +
                        "        \"photo_id\":source_id, \n" +
                        "        \"count\":\"1\", \n" +
                        "        \"sort\":\"asc\", \n" +
                        "        \"access_key\":access_key}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"video\"){\n" +
                        "   first_id = API.video.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"video_id\":source_id, \"count\":\"1\", \"sort\":\"asc\"}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"topic\"){\n" +
                        "    first_id = API.board.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"group_id\":positive_group_id, \n" +
                        "        \"topic_id\":source_id, \"count\":\"1\", \"sort\":\"asc\"}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "var last_id;\n" +
                        "if(source_type == \"post\"){\n" +
                        "    if(comment_id != 0)\n" +
                        "    {\n" +
                        "    last_id = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"desc\", \"comment_id\":comment_id, \"thread_items_count\":\"10\"}).items[0].id;\n" +
                        "    }\n" +
                        "    else\n" +
                        "    {\n" +
                        "    last_id = API.wall.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"post_id\":source_id, \"count\":\"1\", \"sort\":\"desc\", \"thread_items_count\":\"10\"}).items[0].id;\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"photo\"){\n" +
                        "    last_id = API.photos.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"photo_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"video\"){\n" +
                        "    last_id = API.video.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":owner_id, \n" +
                        "        \"video_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "if(source_type == \"topic\"){\n" +
                        "    last_id = API.board.getComments({\"v\":\"" + Constants.API_VERSION + "\",\"group_id\":positive_group_id, \n" +
                        "        \"topic_id\":source_id, \"count\":\"1\", \"sort\":\"desc\"}).items[0].id;\n" +
                        "}\n" +
                        "\n" +
                        "return {\"main\": result, \n" +
                        "    \"first_id\": first_id, \n" +
                        "    \"last_id\": last_id, \n" +
                        "    \"admin_level\": admin_level};", sourceType, ownerId, sourceId, offset, count, sort, startCommentId, thread_id, accessKey, fields]
                    .map(extractResponseWithErrorHandling())
            }
    }
}