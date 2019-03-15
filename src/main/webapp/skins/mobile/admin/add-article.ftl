<#--

    Symphony - A modern community (forum/BBS/SNS/blog) platform written in Java.
    Copyright (C) 2012-2019, b3log.org & hacpai.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

-->
<#include "macro-admin.ftl">
<@admin "addArticle">
<div class="wrapper">
    <div class="module">
        <div class="module-header">
            <h2>${addArticleLabel}</h2>
        </div>
        <div class="module-panel form fn-clear">
            <form action="${servePath}/admin/add-article" method="POST">
                <label>${userNameLabel}</label>
                <input name="userName" type="text" />

                <label>${timeLabel}</label>
                <input name="time" type="datetime-local" />

                <label>${titleLabel}</label>
                <input name="articleTitle" type="text" />

                <label>${tagLabel}</label>
                <input name="articleTags" type="text" />

                <label>${contentLabel}</label>
                <textarea name="articleContent" rows="20"></textarea>


                <br/><br/><br/>
                <button type="submit" class="green fn-right">${submitLabel}</button>
            </form>
        </div>
    </div>   
</div>
</@admin>