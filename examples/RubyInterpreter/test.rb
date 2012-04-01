out = $response.writer
$response.content_type = "text/html"

out.println "<html><body>"
out.println "<h1>Ruby output!</h1>"

out.println "<ul>"

pages = $database.get_view("Pages")
page = pages.get_first_document
while page != nil
	out.println("<li>#{page.get_item_value_string('Title')}</li>")
	
	page = pages.get_next_document(page)
end

out.println "</ul>"

out.println "</body></html>"