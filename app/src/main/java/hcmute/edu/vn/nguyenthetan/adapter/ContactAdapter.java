package hcmute.edu.vn.nguyenthetan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.Contact;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface OnContactActionListener {
        void onCallClick(Contact contact);
        void onSmsClick(Contact contact);
    }

    private List<Contact> contacts = new ArrayList<>();
    private List<Contact> contactsFull = new ArrayList<>();
    private OnContactActionListener listener;

    public ContactAdapter(OnContactActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Contact> data) {
        this.contactsFull = new ArrayList<>(data);
        this.contacts = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        contacts.clear();
        if (query == null || query.trim().isEmpty()) {
            contacts.addAll(contactsFull);
        } else {
            String lower = query.toLowerCase().trim();
            for (Contact c : contactsFull) {
                if (c.getName().toLowerCase().contains(lower) || c.getPhone().contains(lower)) {
                    contacts.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Avatar - chữ cái đầu tiên của tên
        String name = contact.getName();
        String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);

        holder.tvContactName.setText(contact.getName());
        holder.tvContactPhone.setText(contact.getPhone());

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) listener.onCallClick(contact);
        });

        holder.btnSms.setOnClickListener(v -> {
            if (listener != null) listener.onSmsClick(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvContactName, tvContactPhone;
        ImageView btnCall, btnSms;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvContactPhone = itemView.findViewById(R.id.tvContactPhone);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnSms = itemView.findViewById(R.id.btnSms);
        }
    }
}
